package eclosia.eclosia_organization_service.report.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class BulletinSubjectRowDto {

    private UUID studentEnrollmentId;
    private String studentNumber = "";
    private String studentFullName = "";
    private String gender = "";
    private String birthPlace = "";
    private String birthDate = "";
    private String classroomName = "";
    private String enrollmentNumber = "";
    private String photoPath = "";

    private String schoolName = "";
    private String schoolCode = "";
    private String schoolAddress = "";
    private String provinceName = "";
    private String cityName = "";
    private String communeName = "";
    private String principalName = "";
    private String academicYearLabel = "";
    private String bulletinTitle = "";

    private String classRank = "";
    private String studentCount = "";

    /** Pourcentage annuel (colonne TOTAL PTS OBT.). */
    private String percentage = "";
    private String yearPercentage = "";

    private String t1P1Percentage = "";
    private String t1P2Percentage = "";
    private String t1ExamPercentage = "";
    private String t1TotPercentage = "";

    private String t2P1Percentage = "";
    private String t2P2Percentage = "";
    private String t2ExamPercentage = "";
    private String t2TotPercentage = "";

    private String t3P1Percentage = "";
    private String t3P2Percentage = "";
    private String t3ExamPercentage = "";
    private String t3TotPercentage = "";

    private String rowType = "";
    private String domainName = "";
    private String branchName = "";

    /** MAX par période (colonne unique officielle). */
    private String maxPoints = "";

    private String t1P1 = "";
    private String t1P2 = "";
    private String t1ExamMax = "";
    private String t1Exam = "";
    private String t1TrimMax = "";
    private String t1Tot = "";

    private String t2P1 = "";
    private String t2P2 = "";
    private String t2ExamMax = "";
    private String t2Exam = "";
    private String t2TrimMax = "";
    private String t2Tot = "";

    private String t3P1 = "";
    private String t3P2 = "";
    private String t3ExamMax = "";
    private String t3Exam = "";
    private String t3TrimMax = "";
    private String t3Tot = "";

    private String yearMax = "";
    private String yearTot = "";
}
