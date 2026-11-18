package nl.lugatiy.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.security.Principal;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController @RequestMapping("/api/v1")
class QuizController {
 private final JdbcTemplate db;
 QuizController(JdbcTemplate db) { this.db=db; }
 record LevelInput(@NotBlank @Size(max=120) String name, @Size(max=1000) String description, @Min(1) @Max(10) int difficulty) {}
 record Answer(UUID id, String text) {}
 record Question(UUID id, String prompt, List<Answer> answers) {}
 record Quiz(UUID id, String title, String description, UUID levelId, String level, List<Question> questions) {}
 record Selection(@NotNull UUID questionId, @NotNull UUID answerId) {}
 record AttemptInput(@NotEmpty @Size(max=100) List<@Valid Selection> answers) {}
 record Feedback(UUID questionId, boolean correct, String explanation) {}
 record Result(int score, int total, List<Feedback> feedback) {}
 @GetMapping("/levels") List<Map<String,Object>> levels() { return db.queryForList("select id,name,description,difficulty from quiz_levels order by difficulty,name"); }
 @PostMapping("/levels") @ResponseStatus(HttpStatus.CREATED) Map<String,Object> level(@Valid @RequestBody LevelInput in) { UUID id=UUID.randomUUID(); db.update("insert into quiz_levels(id,name,description,difficulty) values (?,?,?,?)",id,in.name().trim(),Objects.toString(in.description(),""),in.difficulty()); return db.queryForMap("select id,name,description,difficulty from quiz_levels where id=?",id); }
 @PutMapping("/levels/{id}") Map<String,Object> editLevel(@PathVariable UUID id, @Valid @RequestBody LevelInput in) { int count=db.update("update quiz_levels set name=?,description=?,difficulty=? where id=?",in.name().trim(),Objects.toString(in.description(),""),in.difficulty(),id); if(count==0) throw new ResponseStatusException(HttpStatus.NOT_FOUND); return db.queryForMap("select id,name,description,difficulty from quiz_levels where id=?",id); }
 @DeleteMapping("/levels/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) void deleteLevel(@PathVariable UUID id) { Integer used=db.queryForObject("select count(*) from quizzes where level_id=?",Integer.class,id); if(used!=null && used>0) throw new ResponseStatusException(HttpStatus.CONFLICT,"Level is in use"); if(db.update("delete from quiz_levels where id=?",id)==0) throw new ResponseStatusException(HttpStatus.NOT_FOUND); }
 @GetMapping("/quizzes") List<Map<String,Object>> quizzes() { return db.queryForList("select q.id,q.title,q.description,l.name as level from quizzes q join quiz_levels l on l.id=q.level_id where q.published order by l.difficulty,q.title"); }
 @GetMapping("/quizzes/{id}") Quiz quiz(@PathVariable UUID id) {
  var rows=db.query("select q.id,q.title,q.description,q.level_id,l.name from quizzes q join quiz_levels l on l.id=q.level_id where q.id=? and q.published",(r,n)->new Object[]{r.getObject(1,UUID.class),r.getString(2),r.getString(3),r.getObject(4,UUID.class),r.getString(5)},id);
  if(rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
  var q=rows.get(0); var questions=db.query("select id,prompt from quiz_questions where quiz_id=? order by position",(r,n)->new Question(r.getObject(1,UUID.class),r.getString(2), db.query("select id,answer_text from quiz_answers where question_id=? order by id",(a,j)->new Answer(a.getObject(1,UUID.class),a.getString(2)),r.getObject(1,UUID.class))),id);
  return new Quiz((UUID)q[0],(String)q[1],(String)q[2],(UUID)q[3],(String)q[4],questions);
 }
 @PostMapping("/quizzes/{id}/attempts") @Transactional Result attempt(@PathVariable UUID id, @Valid @RequestBody AttemptInput input, Principal principal) {
  Quiz quiz=quiz(id); if(quiz.questions().isEmpty() || input.answers().size()!=quiz.questions().size()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Answer all questions exactly once");
  Map<UUID,UUID> submitted=new HashMap<>(); for(Selection s:input.answers()) if(submitted.put(s.questionId(),s.answerId())!=null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Duplicate question");
  List<Feedback> feedback=new ArrayList<>(); int score=0;
  for(Question q:quiz.questions()) { UUID answer=submitted.get(q.id()); if(answer==null || q.answers().stream().noneMatch(a->a.id().equals(answer))) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid answer");
   var rows=db.queryForList("select a.correct,q.explanation from quiz_answers a join quiz_questions q on q.id=a.question_id where a.id=? and q.id=?",answer,q.id()); boolean correct=(Boolean)rows.get(0).get("correct"); if(correct) score++; feedback.add(new Feedback(q.id(),correct,(String)rows.get(0).get("explanation")));
  }
  UUID user=db.queryForObject("select id from app_users where email=?",UUID.class,principal.getName()); db.update("insert into quiz_attempts(id,quiz_id,user_id,score,total) values (?,?,?,?,?)",UUID.randomUUID(),id,user,score,quiz.questions().size()); return new Result(score,quiz.questions().size(),feedback);
 }
 @GetMapping("/progress") List<Map<String,Object>> progress(Principal principal) { return db.queryForList("select q.title,a.score,a.total,a.completed_at from quiz_attempts a join quizzes q on q.id=a.quiz_id join app_users u on u.id=a.user_id where u.email=? order by a.completed_at desc limit 50",principal.getName()); }
}
