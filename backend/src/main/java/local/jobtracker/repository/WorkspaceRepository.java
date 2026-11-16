package local.jobtracker.repository;

import java.util.*;
import local.jobtracker.domain.*;
import org.springframework.data.jpa.repository.*;

public interface WorkspaceRepository extends JpaRepository<Workspace, UUID> {}
