
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;

public class Backup {

    @GetMapping("/admin/backup")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public void backup(HttpServletResponse response) throws IOException {
        response.setHeader("Content-Disposition", "attachment; filename=backup.sql");
        // trigger DB dump
    }

}
