package by.iba.bussiness.notification.service.v1;

import by.iba.bussiness.appointment.Appointment;
import by.iba.bussiness.appointment.AppointmentInstaller;
import by.iba.bussiness.appointment.repository.AppointmentRepository;
import by.iba.bussiness.calendar.rrule.Rrule;
import by.iba.bussiness.calendar.rrule.definer.RruleDefiner;
import by.iba.bussiness.calendar.session.Session;
import by.iba.bussiness.facade.ComplexTemplateSenderFacade;
import by.iba.bussiness.facade.SimpleCalendarSenderFacade;
import by.iba.bussiness.invitation_template.InvitationTemplate;
import by.iba.bussiness.invitation_template.service.InvitationTemplateService;
import by.iba.bussiness.meeting.Meeting;
import by.iba.bussiness.meeting.service.MeetingService;
import by.iba.bussiness.meeting.type.MeetingType;
import by.iba.bussiness.meeting.type.MeetingTypeDefiner;
import by.iba.bussiness.notification.service.SenderService;
import by.iba.bussiness.placeholder.replacer.TemplatePlaceHolderReplacer;
import by.iba.bussiness.placeholder.PlaceHoldersInstaller;
import by.iba.bussiness.sender.MailSendingResponseStatus;
import by.iba.exception.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;

@Service
public class SenderServiceImpl implements SenderService {
    private static final Logger logger = LoggerFactory.getLogger(SenderServiceImpl.class);
    private MeetingService meetingService;
    private InvitationTemplateService invitationTemplateService;
    private AppointmentInstaller appointmentInstaller;
    private MeetingTypeDefiner meetingTypeDefiner;
    private ComplexTemplateSenderFacade complexTemplateSenderFacade;
    private SimpleCalendarSenderFacade simpleCalendarSenderFacade;
    private AppointmentRepository appointmentRepository;
    private RruleDefiner rruleDefiner;
    private PlaceHoldersInstaller placeHoldersInstaller;
    private TemplatePlaceHolderReplacer templatePlaceHolderReplacer;

    @Autowired
    public SenderServiceImpl(MeetingService meetingService,
                             InvitationTemplateService invitationTemplateService,
                             AppointmentInstaller appointmentInstaller,
                             MeetingTypeDefiner meetingTypeDefiner,
                             ComplexTemplateSenderFacade complexTemplateSenderFacade,
                             SimpleCalendarSenderFacade simpleCalendarSenderFacade,
                             AppointmentRepository appointmentRepository,
                             RruleDefiner rruleDefiner,
                             PlaceHoldersInstaller placeHoldersInstaller,
                             TemplatePlaceHolderReplacer templatePlaceHolderReplacer) {
        this.meetingService = meetingService;
        this.invitationTemplateService = invitationTemplateService;
        this.appointmentInstaller = appointmentInstaller;
        this.meetingTypeDefiner = meetingTypeDefiner;
        this.complexTemplateSenderFacade = complexTemplateSenderFacade;
        this.simpleCalendarSenderFacade = simpleCalendarSenderFacade;
        this.appointmentRepository = appointmentRepository;
        this.rruleDefiner = rruleDefiner;
        this.placeHoldersInstaller = placeHoldersInstaller;
        this.templatePlaceHolderReplacer = templatePlaceHolderReplacer;
    }

    @Override
    public List<MailSendingResponseStatus> sendCalendarToAllEnrollmentsOfMeeting(HttpServletRequest request, String meetingId) {
        Meeting meeting = meetingService.getMeetingById(request, meetingId);
        if (meeting == null) {
            logger.info("Can't find meeting in ec3 with meetingId: " + meetingId);
            throw new ServiceException("Can't find meeting with id " + meetingId);
        }
        String invitationTemplateKey = meeting.getInvitationTemplate();
        if (invitationTemplateKey.isEmpty()) {
            logger.error("Can't enroll learners to this event, cause can't find some invitation template by meeting id: " + meetingId);
            throw new ServiceException("Meeting " + meetingId + " doesn't have learner invitation template");
        }
        InvitationTemplate invitationTemplate = invitationTemplateService.getInvitationTemplateByCode(request, invitationTemplateKey);
        Map<String, String> placeHolders = placeHoldersInstaller.installPlaceHoldersMap(meeting);
        InvitationTemplate modifiedInvTemplate = templatePlaceHolderReplacer.replaceTemplatePlaceHolders(placeHolders, invitationTemplate);
        Appointment oldAppointment = appointmentRepository.getByMeetingId(new BigInteger(meetingId));
        meeting.setPlainDescription("Plain description"); // mock
        Appointment newAppointment = appointmentInstaller.installAppointment(meeting, modifiedInvTemplate, oldAppointment);
        List<MailSendingResponseStatus> mailSendingResponseStatusList;
        List<Session> newAppSessions = newAppointment.getSessionList();
        MeetingType newAppointmentMeetingType = meetingTypeDefiner.defineMeetingType(newAppSessions);
        if (newAppointmentMeetingType.equals(MeetingType.SIMPLE)) {
            mailSendingResponseStatusList = simpleCalendarSenderFacade.sendCalendar(newAppointment, oldAppointment);
        } else {
            mailSendingResponseStatusList = complexTemplateSenderFacade.sendTemplate(newAppointment, oldAppointment);
        }
        return mailSendingResponseStatusList;
    }
}