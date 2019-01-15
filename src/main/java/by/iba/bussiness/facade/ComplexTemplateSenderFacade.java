package by.iba.bussiness.facade;

import by.iba.bussiness.appointment.Appointment;
import by.iba.bussiness.calendar.CalendarFactory;
import by.iba.bussiness.calendar.CalendarStatus;
import by.iba.bussiness.calendar.creator.installer.CalendarAttendeesInstaller;
import by.iba.bussiness.calendar.date.helper.model.DateHelper;
import by.iba.bussiness.enrollment.Enrollment;
import by.iba.bussiness.enrollment.EnrollmentsInstaller;
import by.iba.bussiness.enrollment.repository.EnrollmentRepository;
import by.iba.bussiness.enrollment.service.v1.EnrollmentServiceImpl;
import by.iba.bussiness.enrollment.status.EnrollmentStatus;
import by.iba.bussiness.meeting.MeetingType;
import by.iba.bussiness.sender.MailSendingResponseStatus;
import by.iba.bussiness.sender.MessageSender;
import by.iba.bussiness.template.Template;
import by.iba.bussiness.template.installer.TemplateInstaller;
import by.iba.bussiness.template.installer.TemplateStatusInstaller;
import net.fortuna.ical4j.model.Calendar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

@Component
public class ComplexTemplateSenderFacade {

    private static final Logger logger = LoggerFactory.getLogger(EnrollmentServiceImpl.class);
    private MessageSender messageSender;
    private EnrollmentsInstaller enrollmentsInstaller;
    private EnrollmentRepository enrollmentRepository;
    private TemplateStatusInstaller templateStatusInstaller;
    private TemplateInstaller templateInstaller;
    private CalendarFactory calendarFactory;
    private CalendarAttendeesInstaller calendarAttendeesInstaller;


    @Autowired
    public ComplexTemplateSenderFacade(MessageSender messageSender,
                                       EnrollmentsInstaller enrollmentsInstaller,
                                       EnrollmentRepository enrollmentRepository,
                                       TemplateStatusInstaller templateStatusInstaller,
                                       TemplateInstaller templateInstaller,
                                       CalendarFactory calendarFactory,
                                       CalendarAttendeesInstaller calendarAttendeesInstaller) {
        this.messageSender = messageSender;
        this.enrollmentsInstaller = enrollmentsInstaller;
        this.enrollmentRepository = enrollmentRepository;
        this.templateStatusInstaller = templateStatusInstaller;
        this.templateInstaller = templateInstaller;
        this.calendarFactory = calendarFactory;
        this.calendarAttendeesInstaller = calendarAttendeesInstaller;
    }

    public List<MailSendingResponseStatus> sendTemplate(Appointment appointment, Appointment oldAppointment, DateHelper oldMeetingDateHelper) {
        BigInteger meetingId = appointment.getMeetingId();
        List<MailSendingResponseStatus> mailSendingResponseStatusList = new ArrayList<>();
        List<Enrollment> enrollmentList = enrollmentRepository.getAllByParentId(meetingId);
        Template template = new Template();
        templateInstaller.installCommontPartsOfTemplate(appointment, oldAppointment, template);
        boolean isRecentMeetingRecurr = false;
        if (oldMeetingDateHelper != null) {
            isRecentMeetingRecurr = oldMeetingDateHelper.getMeetingType().equals(MeetingType.RECURRENCE);
        }
        for (Enrollment enrollment : enrollmentList) {
            if (isRecentMeetingRecurr) {
                Calendar cancelCalendar = calendarFactory.createCancelCalendarTemplate(oldMeetingDateHelper, appointment, enrollment);
                calendarAttendeesInstaller.addAttendeeToCalendar(enrollment, cancelCalendar);
                messageSender.sendCalendarToLearner(cancelCalendar);
            }
            if (CalendarStatus.CANCELLED.equals(enrollment.getCalendarStatus())
                    && EnrollmentStatus.CANCELLED.equals(enrollment.getStatus())) {
                MailSendingResponseStatus badMailSendingResponseStatus =
                        new MailSendingResponseStatus(false, "User has cancelled status. ", enrollment.getUserEmail());
                mailSendingResponseStatusList.add(badMailSendingResponseStatus);
            } else {
                templateStatusInstaller.installTemplateType(enrollment, appointment, template);
                if (template.getType() == null) {
                    MailSendingResponseStatus badMailSendingResponseStatus =
                            new MailSendingResponseStatus(false, "User has already updated version. ", enrollment.getUserEmail());
                    mailSendingResponseStatusList.add(badMailSendingResponseStatus);
                    logger.info("Don't need to send message to " + enrollment.getUserEmail());
                } else {
                    String userEmail = enrollment.getUserEmail();
                    MailSendingResponseStatus mailSendingResponseStatus = messageSender.sendTemplate(template, userEmail);
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