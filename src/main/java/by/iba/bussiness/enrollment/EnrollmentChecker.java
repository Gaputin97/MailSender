package by.iba.bussiness.enrollment;

import by.iba.bussiness.appointment.Appointment;
import by.iba.bussiness.calendar.attendee.Learner;
import by.iba.bussiness.enrollment.repository.EnrollmentRepository;
import by.iba.bussiness.enrollment.service.EnrollmentService;
import by.iba.bussiness.meeting.Meeting;
import by.iba.exception.ServiceException;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.math.BigInteger;
import java.util.List;

@Component
public class EnrollmentChecker {
    private static final Logger logger = LoggerFactory.getLogger(EnrollmentChecker.class);
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    public EnrollmentChecker(EnrollmentRepository enrollmentRepository) {
        this.enrollmentRepository = enrollmentRepository;
    }

    public boolean wasChangedStatus(Learner learner, BigInteger meetingId) {
        boolean wasChanged;
        String email = learner.getEmail();
        Enrollment enrollment = enrollmentRepository.getByEmailAndParentId(meetingId, email);
        boolean isExistLocalEnrollment = enrollment != null;
        if (isExistLocalEnrollment) {
            EnrollmentType enrollmentType = enrollment.getEnrollmentType();
            EnrollmentType learnerType = learner.getEnrollmentType();
            if (enrollmentType != learnerType) {
                wasChanged = false;
            } else {
                wasChanged = true;
            }
            logger.info("Enrollment with meeting id " + meetingId + " and email " + email + " and enrollment type " + enrollmentType + " exists. ");
        } else {
            wasChanged = false;
        }
        return wasChanged;
    }
}

