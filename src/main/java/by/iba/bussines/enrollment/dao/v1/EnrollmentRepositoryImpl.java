package by.iba.bussines.enrollment.dao.v1;

import by.iba.bussines.enrollment.dao.EnrollmentRepository;
import by.iba.bussines.enrollment.model.Enrollment;
import by.iba.bussines.exception.DaoException;
import by.iba.bussines.status.InsertStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

@Repository
public class EnrollmentRepositoryImpl implements EnrollmentRepository {
    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public InsertStatus save(Enrollment enrollment) {
        try {
            mongoTemplate.save(enrollment);
        } catch (Exception e) {
            new DaoException(e.getMessage());
        }
        InsertStatus enrollmentInsertStatus = new InsertStatus();
        enrollmentInsertStatus.setMessage("Enrollment was inserted successfully");
        return enrollmentInsertStatus;
    }

    @Override
    public Enrollment getByEmailAbdMeetingId(String parentId, String userEmail) {
        Query query = new Query(Criteria.where("parentId").is(parentId).and("userEmail").is(userEmail));
        Enrollment enrollment = mongoTemplate.findOne(query, Enrollment.class);
        if (enrollment == null) {
            throw new DaoException("There are no enrollment with parentId " + parentId + " and user email " + userEmail);
        }
        return enrollment;
    }
}
