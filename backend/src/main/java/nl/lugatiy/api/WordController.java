package nl.lugatiy.api;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.UUID;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
@RestController @RequestMapping("/api/v1/words")
class WordController {
 private final WordRepository words;
 WordController(WordRepository words) { this.words=words; }
 record WordInput(@NotBlank @Size(max=255) String arabic, @NotBlank @Size(max=255) String dutch, @Size(max=100) String partOfSpeech, @Size(max=100) String root, @Size(max=5000) String exampleArabic, @Size(max=5000) String exampleDutch) {}
 record WordView(UUID id, String arabic, String dutch, String partOfSpeech, String root, String exampleArabic, String exampleDutch) {
  static WordView from(Word w) { return new WordView(w.id,w.arabic,w.dutch,w.partOfSpeech,w.root,w.exampleArabic,w.exampleDutch); }
 }
 @GetMapping Page<WordView> search(@RequestParam(defaultValue="") @Size(max=255) String q, @RequestParam(defaultValue="0") int page) { return words.search(Arabic.normalize(q), PageRequest.of(Math.max(0,page), 24)).map(WordView::from); }
 @PostMapping @ResponseStatus(HttpStatus.CREATED) WordView create(@Valid @RequestBody WordInput input) { Word w=new Word(); w.id=UUID.randomUUID(); update(w,input); return WordView.from(words.save(w)); }
 @PutMapping("/{id}") WordView edit(@PathVariable UUID id, @Valid @RequestBody WordInput input) { Word w=words.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND)); update(w,input); return WordView.from(words.save(w)); }
 @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) void delete(@PathVariable UUID id) { Word w=words.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND)); words.delete(w); }
 private void update(Word w, WordInput input) { w.arabic=input.arabic().trim(); w.arabicNormalized=Arabic.normalize(input.arabic()); w.dutch=input.dutch().trim(); w.partOfSpeech=input.partOfSpeech(); w.root=Arabic.normalize(input.root()); w.exampleArabic=input.exampleArabic(); w.exampleDutch=input.exampleDutch(); }
}
