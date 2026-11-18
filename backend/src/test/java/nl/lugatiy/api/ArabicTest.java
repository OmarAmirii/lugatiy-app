package nl.lugatiy.api;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ArabicTest {
 @Test void removesMarksWithoutChangingStoredText() { String original="أَسَدٌ"; assertEquals("أسد", Arabic.normalize(original)); assertEquals("أَسَدٌ", original); }
 @Test void handlesShaddaAndTatweel() { assertEquals("مدرسة",Arabic.normalize("مَـدْرَسَةٌ")); }
}
