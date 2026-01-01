package halo.corebridge.jobposting.repository;

import halo.corebridge.jobposting.model.entity.Jobposting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobpostingRepository extends JpaRepository<Jobposting, Long> {
}
