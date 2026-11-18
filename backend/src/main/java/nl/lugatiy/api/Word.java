package nl.lugatiy.api;
import jakarta.persistence.*;
import java.util.UUID;
@Entity @Table(name="words")
class Word {
 @Id UUID id;
 @Column(nullable=false) String arabic;
 @Column(name="arabic_normalized", nullable=false) String arabicNormalized;
 @Column(nullable=false) String dutch;
 @Column(name="part_of_speech") String partOfSpeech;
 String root;
 @Column(name="example_arabic") String exampleArabic;
 @Column(name="example_dutch") String exampleDutch;
 protected Word() {}
}
