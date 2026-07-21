package eclosia.eclosia_organization_service.report.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class BulletinSubjectRowDto {

    private UUID studentEnrollmentId;
    private String studentNumber = "";
    private String studentFullName = "";
    private String birthPlace = "";
    private String birthDate = "";
    private String classroomName = "";
    private String enrollmentNumber = "";
    private String photoPath = "";

    private String schoolName = "";
    private String schoolCode = "";
    private String schoolAddress = "";
    private String principalName = "";
    private String academicYearLabel = "";

    private String classRank = "";
    private String schoolRank = "";

    private String rowType = "";
    private String domainName = "";
    private String branchName = "";

    private String maxPoints = "";

    private String t1Max = "";
    private String t1P1 = "";
    private String t1P2 = "";
    private String t1Exam = "";
    private String t1Moy = "";
    private String t1Rr = "";
    private String t1Tot = "";

    private String t2Max = "";
    private String t2P1 = "";
    private String t2P2 = "";
    private String t2Exam = "";
    private String t2Moy = "";
    private String t2Rr = "";
    private String t2Tot = "";

    private String t3Max = "";
    private String t3P1 = "";
    private String t3P2 = "";
    private String t3Exam = "";
    private String t3Moy = "";
    private String t3Rr = "";
    private String t3Tot = "";

    private String yearMax = "";
    private String yearTot = "";

    private String classAvg = "";
}
