package University.exam.service;

import University.exam.Entity.Question;
import University.exam.dto.Section;
import University.exam.repository.QuestionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class SectionService {

    @Autowired
    private QuestionRepository questionRepository;

    public List<Section> getSectionsByPaperId(Long paperId) {
        List<Question> questions = questionRepository.findByPaperId(paperId);
        return groupQuestionsIntoSections(questions);
    }

    public List<Section> getSectionsByExamId(Long examId) {
        List<Question> questions = questionRepository.findByExamId(examId);
        return groupQuestionsIntoSections(questions);
    }

    private List<Section> groupQuestionsIntoSections(List<Question> questions) {
        if (questions == null || questions.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, List<Question>> map = new LinkedHashMap<>();
        for (Question q : questions) {
            String group = q.getQuestionGroup() != null && !q.getQuestionGroup().isEmpty() 
                            ? q.getQuestionGroup() 
                            : "Section 1";
            map.computeIfAbsent(group, k -> new ArrayList<>()).add(q);
        }

        List<Section> sections = new ArrayList<>();
        for (Map.Entry<String, List<Question>> entry : map.entrySet()) {
            sections.add(new Section(entry.getKey(), entry.getValue()));
        }
        return sections;
    }
}
