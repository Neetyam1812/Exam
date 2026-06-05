package University.exam.service;

import University.exam.Entity.Result;
import University.exam.repository.ResultRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Service
public class ResultService {
    @Autowired
    private ResultRepository resultRepository;

    public List<Result> getFilteredResults(Long adminId, String division, String semester, String subjectName, String status) {
        if (subjectName == null || subjectName.isEmpty() || semester == null || semester.isEmpty()) {
            // Require at least subject and semester to be selected
            return java.util.Collections.emptyList();
        }
        
        List<Result> results;
        if (division != null && !division.isEmpty()) {
            results = resultRepository.findByAdminIdAndSubjectAndSemesterAndDivision(adminId, subjectName, semester, division);
        } else {
            results = resultRepository.findByAdminIdAndSubjectAndSemester(adminId, subjectName, semester);
        }

        if (status != null && !status.isEmpty() && !"all".equalsIgnoreCase(status)) {
            List<Result> filtered = new java.util.ArrayList<>();
            for (Result r : results) {
                if (status.equalsIgnoreCase(r.getResultStatus())) {
                    filtered.add(r);
                }
            }
            return filtered;
        }

        return results;
    }

    public List<String> getDistinctSubjects(Long adminId) {
        return resultRepository.findDistinctSubjectNamesByAdminId(adminId);
    }

    public List<String> getDistinctSemesters(Long adminId) {
        return resultRepository.findDistinctSemestersByAdminId(adminId);
    }

    public List<String> getDistinctDivisions(Long adminId) {
        return resultRepository.findDistinctDivisionsByAdminId(adminId);
    }
}
