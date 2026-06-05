package University.exam.dto;

import University.exam.Entity.Question;
import java.util.List;

public class Section {
    private String name;
    private List<Question> questions;

    public Section() {
    }

    public Section(String name, List<Question> questions) {
        this.name = name;
        this.questions = questions;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Question> getQuestions() {
        return questions;
    }

    public void setQuestions(List<Question> questions) {
        this.questions = questions;
    }
}
