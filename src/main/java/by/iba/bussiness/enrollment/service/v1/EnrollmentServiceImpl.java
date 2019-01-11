package by.iba.bussiness.enrollment.service.v1;

import by.iba.bussiness.appointment.Appointment;
import by.iba.bussiness.appointment.AppointmentInstaller;
import by.iba.bussiness.calendar.CalendarStatus;
import by.iba.bussiness.calendar.learner.Learner;
import by.iba.bussiness.calendar.creator.CalendarCreator;
import by.iba.bussiness.calendar.creator.installer.CalendarAttendeesInstaller;
import by.iba.bussiness.calendar.date.DateHelperDefiner;
import by.iba.bussiness.calendar.date.model.DateHelper;
import by.iba.bussiness.enrollment.Enrollment;
import by.iba.bussiness.enrollment.EnrollmentLearnerStatus;
import by.iba.bussiness.enrollment.EnrollmentsInstaller;
import by.iba.bussiness.enrollment.repository.EnrollmentRepository;
import by.iba.bussiness.enrollment.service.EnrollmentService;
import by.iba.bussiness.invitation_template.InvitationTemplate;
import by.iba.bussiness.invitation_template.service.InvitationTemplateService;
import by.iba.bussiness.meeting.Meeting;
import by.iba.bussiness.meeting.service.MeetingService;
import by.iba.bussiness.sender.MessageSender;
import by.iba.bussiness.sender.MailSendingResponseStatus;
import by.iba.bussiness.token.model.JavaWebToken;
import by.iba.bussiness.token.service.TokenService;
import by.iba.exception.ServiceException;
import net.fortuna.ical4j.model.Calendar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.ParameterizedTypeReference;

import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

@Service
@PropertySource("endpoint.properties")
public class EnrollmentServiceImpl implements EnrollmentService {
    private static final Logger logger = LoggerFactory.getLogger(EnrollmentServiceImpl.class);
    private TokenService tokenService;
    private RestTemplate restTemplate;
    private MeetingService meetingService;
    private CalendarAttendeesInstaller calendarAttendeesInstaller;
    private MessageSender messageSender;
    private InvitationTemplateService invitationTemplateService;
    private EnrollmentsInstaller enrollmentsInstaller;
    private EnrollmentRepository enrollmentRepository;
    private AppointmentInstaller appointmentInstaller;
    private CalendarCreator calendarCreator;
    private DateHelperDefiner dateHelperDefiner;

    @Value("${enrollment_by_email_and_meeting_id_endpoint}")
    private String ENDPOINT_FIND_ENROLLMENT_BY_PARENT_ID_AND_EMAIL;

    @Value("${enrollment_by_parent_id_endpoint}")
    private String ENDPOINT_FIND_ENROLLMENT_BY_PARENT_ID;

    @Autowired
    public EnrollmentServiceImpl(TokenService tokenService,
                                 RestTemplate restTemplate,
                                 MeetingService meetingService,
                                 CalendarAttendeesInstaller calendarAttendeesInstaller,
                                 MessageSender messageSender,
                                 InvitationTemplateService invitationTemplateService,
                                 EnrollmentsInstaller enrollmentsInstaller,
                                 EnrollmentRepository enrollmentRepository,
                                 AppointmentInstaller appointmentInstaller, CalendarCreator calendarCreator,
                                 DateHelperDefiner dateHelperDefiner) {
        this.tokenService = tokenService;
        this.restTemplate = restTemplate;
        this.meetingService = meetingService;
        this.calendarAttendeesInstaller = calendarAttendeesInstaller;
        this.messageSender = messageSender;
        this.invitationTemplateService = invitationTemplateService;
        this.enrollmentsInstaller = enrollmentsInstaller;
        this.enrollmentRepository = enrollmentRepository;
        this.appointmentInstaller = appointmentInstaller;
        this.calendarCreator = calendarCreator;
        this.dateHelperDefiner = dateHelperDefiner;
    }

    @Override
    public List<EnrollmentLearnerStatus> enrollLearners(HttpServletRequest request,
                                                        String meetingId,
                                                        List<Learner> learners) {
        Meeting meeting = meetingService.getMeetingById(request, meetingId);
        String invitationTemplateKey = meeting.getInvitationTemplate();
        if (invitationTemplateKey.isEmpty()) {
            logger.error("Can't enroll learners to this event, cause can't find some invitation template by meeting id: " + meetingId);
            throw new ServiceException("Meeting " + meetingId + " doesn't have learner invitation template");
        }
        return enrollmentsInstaller.installEnrollmentsByLearners(learners, meetingId);
    }

    @Override
    public List<MailSendingResponseStatus> sendCalendarToAllEnrollmentsOfMeeting(HttpServletRequest request, String meetingId) {
        Meeting meeting = meetingService.getMeetingById(request, meetingId);
        String invitationTemplateKey = meeting.getInvitationTemplate();
        if (invitationTemplateKey.isEmpty()) {
            logger.error("Can't enroll learners to this event, cause can't find some invitation template by meeting id: " + meetingId);
            throw new ServiceException("Meeting " + meetingId + " doesn't have learner invitation template");
        }
        InvitationTemplate invitationTemplate = invitationTemplateService.getInvitationTemplateByCode(request, invitationTemplateKey);
        Appointment appointment = appointmentInstaller.installAppointment(meeting, invitationTemplate);
        BigInteger bigIntegerMeetingId = new BigInteger(meetingId);
        List<Enrollment> enrollmentList = enrollmentRepository.getAllByParentId(bigIntegerMeetingId);
        List<MailSendingResponseStatus> mailSendingResponseStatusList = new ArrayList<>();
        DateHelper dateHelper = dateHelperDefiner.defineDateHelper(appointment.getTimeSlots());
        for (Enrollment enrollment : enrollmentList) {
            if (enrollment.getCalendarStatus().equals(CalendarStatus.CANCELLED)
                    && enrollment.getStatus().equals(CalendarStatus.CANCELLED)) {
                MailSendingResponseStatus badMailSendingResponseStatus =
                        new MailSendingResponseStatus(false, "User has cancelled status. ", enrollment.getUserEmail());
                mailSendingResponseStatusList.add(badMailSendingResponseStatus);
            } else {
                Calendar calendar = calendarCreator.createCalendar(enrollment, appointment, dateHelper);
                if (calendar == null) {
                    MailSendingResponseStatus badMailSendingResponseStatus =
                            new MailSendingResponseStatus(false, "User has already updated version. ", enrollment.getUserEmail());
                    mailSendingResponseStatusList.add(badMailSendingResponseStatus);
                    logger.info("Don't need to send message to " + enrollment.getUserEmail());
                } else {
                    calendarAttendeesInstaller.addAttendeeToCalendar(enrollment, calendar);
                    MailSendingResponseStatus mailSendingResponseStatus = messageSender.sendCalendarToLearner(calendar);
                    mailSendingResponseStatusList.add(mailSendingResponseStatus);
                    if (mailSendingResponseStatus.isDelivered()) {
                        enrollmentsInstaller.installEnrollmentCalendarFields(enrollment, appointment);
                    }
                }
            }
        }
        return mailSendingResponseStatusList;
    }
}