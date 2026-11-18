package nl.lugatiy.api;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
class Mp4ValidatorTest {
 @TempDir Path dir;
 @Test void acceptsWellFormedTopLevelBoxes() throws Exception {Path file=dir.resolve("valid.mp4");Files.write(file,new byte[]{0,0,0,16,'f','t','y','p','i','s','o','m',0,0,0,0,0,0,0,9,'m','o','o','v',0,0,0,0,9,'m','d','a','t',0});assertTrue(Mp4Validator.valid(file));}
 @Test void rejectsTruncationAndMissingMovie() throws Exception {Path file=dir.resolve("invalid.mp4");Files.write(file,new byte[]{0,0,0,24,'f','t','y','p','i','s','o','m'});assertFalse(Mp4Validator.valid(file));}
}
