package by.iba.bussiness.enrollment;

import by.iba.bussiness.appointment.Appointment;
import by.iba.bussiness.appointment.AppointmentHandler;
import by.iba.bussiness.calendar.learner.Learner;
import by.iba.bussiness.enrollment.service.EnrollmentService;
import by.iba.bussiness.enrollment.status.EnrollmentCalendarStatusDefiner;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@org.springframework.stereotype.Component
public class EnrollmentsInstaller {
    private EnrollmentService enrollmentService;
    private EnrollmentChecker enrollmentChecker;
    private EnrollmentCalendarStatusDefiner enrollmentCalendarStatusDefiner;
    private AppointmentHandler appointmentHandler;

    @Autowired
    public EnrollmentsInstaller(EnrollmentService enrollmentService,
                                EnrollmentChecker enrollmentChecker,
                                EnrollmentCalendarStatusDefiner enrollmentCalendarStatusDefiner,
                                AppointmentHandler appointmentHandler) {
        this.enrollmentService = enrollmentService;
        this.enrollmentChecker = enrollmentChecker;
        this.enrollmentCalendarStatusDefiner = enrollmentCalendarStatusDefiner;
        this.appointmentHandler = appointmentHandler;
    }

    public List<EnrollmentLearnerStatus> installEnrollmentsByLearners(List<Learner> learners, String meetingId) {
        List<EnrollmentLearnerStatus> enrollmentLearnerStatuses = new ArrayList<>(learners.size());
        BigInteger bigIntegerMeetingId = new BigInteger(meetingId);
        for (Learner learner : learners) {
            String email = learner.getEmail();
            String enrollmentStatus = learner.getEnrollmentStatus();
            Enrollment oldEnrollment = enrollmentService.getByEmailAndParentIdAndType(bigIntegerMeetingId, email, enrollmentStatus);
            if (oldEnrollment == null) {
                oldEnrollment = enrollmentService.getByEmailAndParentId(bigIntegerMeetingId, email);
                if (enrollmentChecker.wasChangedStatus(oldEnrollment, learner)) {
                    oldEnrollment.setStatus(enrollmentStatus);
                    enrollmentService.save(oldEnrollment);
                    EnrollmentLearnerStatus enrollmentLearnerStatus =
                            new EnrollmentLearnerStatus(true, "Enrollment was modified. ", learner.getEmail());
                    enrollmentLearnerStatuses.add(enrollmentLearnerStatus);
                } else {
                    Enrollment newEnrollment = new Enrollment();
                    newEnrollment.setStatus(enrollmentStatus);
                    newEnrollment.setParentId(bigIntegerMeetingId);
                    newEnrollment.setUserEmail(email);
                    newEnrollment.setCurrentCalendarUid(UUID.randomUUID().toString());
                    enrollmentService.save(newEnrollment);
                    EnrollmentLearnerStatus enrollmentLearnerStatus =
                            new EnrollmentLearnerStatus(true, "Enrollment was createD. ", learner.getEmail());
                    enrollmentLearnerStatuses.add(enrollmentLearnerStatus);
                }
            } else {
                EnrollmentLearnerStatus enrollmentLearnerStatus =
                        new EnrollmentLearnerStatus(false, "Enrollment already exists. ", learner.getEmail());
                enrollmentLearnerStatuses.add(enrollmentLearnerStatus);
            }
        }
        return enrollmentLearnerStatuses;
    }

    public void installEnrollmentCalendarFields(Enrollment enrollment, Appointment appointment) {
        int maximumIndex = appointmentHandler.getMaximumIndex(appointment);
        String calendarStatus = enrollmentCalendarStatusDefiner.defineEnrollmentCalendarStatus(enrollment);
        enrollment.setCalendarStatus(calendarStatus);
        enrollment.setCalendarVersion(Integer.toString(maximumIndex));
        enrollmentService.save(enrollment);
    }
}
