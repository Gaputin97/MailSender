package by.iba.bussiness.enrollment.status;

import by.iba.bussiness.calendar.learner.Learner;
import by.iba.bussiness.enrollment.Enrollment;
import by.iba.bussiness.enrollment.service.EnrollmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class EnrollmentStatusChecker {
    private static final Logger logger = LoggerFactory.getLogger(EnrollmentStatusChecker.class);
    private EnrollmentService enrollmentService;

    @Autowired
    public EnrollmentStatusChecker(EnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
    }

    public boolean wasChangedStatus(Enrollment enrollment, Learner learner) {
        boolean wasChanged;
        boolean isExistLocalEnrollment = enrollment != null;
        if (isExistLocalEnrollment) {
            String enrollmentStatus = enrollment.getStatus();
            String learnerStatus = learner.getEnrollmentStatus();
            wasChanged = !enrollmentStatus.equals(learnerStatus);
        } else {
            wasChanged = false;
        }
        return wasChanged;
    }

    public boolean doAllEnrollmentHaveCancelledStatus(List<Enrollment> enrollments) {
        return enrollments.stream().map(Enrollment::getStatus).allMatch(EnrollmentStatus.CANCELLED::equals);
    }

    public boolean doAllEnrollmentHaveConfirmedStatus(List<Enrollment> enrollments) {
        return enrollments.stream().map(Enrollment::getStatus).allMatch(EnrollmentStatus.CONFIRMED::equals);
    }
}
