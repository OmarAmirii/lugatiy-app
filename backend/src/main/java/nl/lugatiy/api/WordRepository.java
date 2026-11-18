package nl.lugatiy.api;
import java.util.UUID;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
interface WordRepository extends JpaRepository<Word, UUID> {
 @Query("select w from Word w where lower(w.arabicNormalized) like lower(concat('%', :q, '%')) or lower(w.dutch) like lower(concat('%', :q, '%')) or lower(coalesce(w.root, '')) like lower(concat('%', :q, '%')) order by w.dutch")
 Page<Word> search(@Param("q") String query, Pageable pageable);
}
