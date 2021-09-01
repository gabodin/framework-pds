package imd.ufrn.br.cashbooks.repository;

import imd.ufrn.br.cashbooks.model.Cliente;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long>{

}
