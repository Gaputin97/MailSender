package by.iba.bussiness.sender;

import by.iba.bussiness.calendar.creator.CalendarTextEditor;
import by.iba.bussiness.template.Template;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.Attendee;
import net.fortuna.ical4j.model.property.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import javax.activation.DataHandler;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.IOException;
import java.net.URL;

@org.springframework.stereotype.Component
public class MessageSender {
    private static final Logger logger = LoggerFactory.getLogger(MessageSender.class);
    private JavaMailSender javaMailSender;
    private CalendarTextEditor calendarTextEditor;
    private Configuration freeMarkerConfiguration;

    @Autowired
    public MessageSender(JavaMailSender javaMailSender,
                         CalendarTextEditor calendarTextEditor,
                         Configuration freeMarkerConfiguration) {
        this.javaMailSender = javaMailSender;
        this.calendarTextEditor = calendarTextEditor;
        this.freeMarkerConfiguration = freeMarkerConfiguration;
    }

    public MailSendingResponseStatus sendCalendarToLearner(Calendar calendar, String richDescription) {
        MimeMessage message;
        VEvent event = (VEvent) calendar.getComponents().getComponent(Component.VEVENT);
        Attendee attendee = event.getProperties().getProperty(Property.ATTENDEE);
        String userEmail = attendee.getCalAddress().toString();
        String editedUserEmail = calendarTextEditor.editUserEmail(userEmail);
        MailSendingResponseStatus mailSendingResponseStatus;
        try {
            message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            Method method = calendar.getMethod();
            String stringMethod = calendarTextEditor.replaceColonToEqual(method.toString());

            helper.setTo(editedUserEmail);

            MimeBodyPart iCalInline = new MimeBodyPart();
            iCalInline.setHeader("Content-ID", "calendar_part");
            iCalInline.setHeader("Content-Disposition", "inline");
            iCalInline.setHeader("Content-Transfer-Encoding", "base64");
            iCalInline.setContent(calendar.toString(), "text/calendar;charset=utf-8;" + stringMethod);
            iCalInline.setFileName("inlineCalendar.ics");

            MimeBodyPart htmlInline = new MimeBodyPart();
            htmlInline.setHeader("Content-ID", "rich_description");
            htmlInline.setHeader("Content-Disposition", "inline");
            htmlInline.setHeader("Content-Transfer-Encoding", "base64");
            htmlInline.setContent(richDescription, "text/html;charset=utf-8");

            MimeMultipart multipart = new MimeMultipart();
            multipart.addBodyPart(htmlInline);
            multipart.addBodyPart(iCalInline);
            message.setContent(multipart);
            javaMailSender.send(message);

            logger.info("Message was sent to " + editedUserEmail);
            mailSendingResponseStatus = new MailSendingResponseStatus(true, "Calendar was sent successfully", editedUserEmail);
        } catch (MessagingException e) {
            logger.error("Error while trying to send message", e);
            mailSendingResponseStatus = new MailSendingResponseStatus(false, "Calendar was not delivered", editedUserEmail);
        }
        return mailSendingResponseStatus;
    }

    public MailSendingResponseStatus sendTemplate(Template template, String userEmail) {
        MimeMessage message;
        MailSendingResponseStatus mailSendingResponseStatus;
        try {
            message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message,
                    true,
                    "utf-8");
            URL url = new URL("https://preview.ibb.co/hXyhQL/Meeting.jpg");
            //helper.addAttachment("pic.png", new URLDataSource(url));
            helper.setTo(userEmail);
            helper.setFrom(template.getOwner().getEmail());
            freemarker.template.Template messageTemplate = freeMarkerConfiguration.getTemplate("message.html");
            String html = FreeMarkerTemplateUtils.processTemplateIntoString(messageTemplate, template);
            message = javaMailSender.createMimeMessage();
            Multipart multipart = new MimeMultipart();
            MimeMessageHelper messageHelper = new MimeMessageHelper(message, true);
            messageHelper.setTo(userEmail);
            MimeBodyPart htmlPart = new MimeBodyPart();
            htmlPart.setContent(html, "text/html; charset=UTF-8");

            MimeBodyPart inlineImage = new MimeBodyPart();
            inlineImage.setContentID("<pic>");
            inlineImage.setHeader("Content-Disposition", "inline");
            inlineImage.setDataHandler(new DataHandler(url));

            multipart.addBodyPart(htmlPart);
            multipart.addBodyPart(inlineImage);
            message.setContent(multipart);

            javaMailSender.send(message);
            logger.info("Message was sent to " + userEmail);
            mailSendingResponseStatus = new MailSendingResponseStatus(true, "Message was sent successfully", userEmail);
        } catch (MessagingException | TemplateException | IOException e) {
            logger.error("Error while trying to send message", e);
            mailSendingResponseStatus = new MailSendingResponseStatus(false, "Message was not delivered", userEmail);
        }
        return mailSendingResponseStatus;
    }
}