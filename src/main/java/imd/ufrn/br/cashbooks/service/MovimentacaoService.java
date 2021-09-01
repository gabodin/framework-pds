package imd.ufrn.br.cashbooks.service;



import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.persistence.EntityNotFoundException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;

import imd.ufrn.br.cashbooks.interfaces.ICategorizarAutomaticamente;
import imd.ufrn.br.cashbooks.interfaces.IGerarRelatorio;
import imd.ufrn.br.cashbooks.interfaces.IRestricoesComprasPrazo;
import imd.ufrn.br.cashbooks.model.Cliente;
import imd.ufrn.br.cashbooks.model.Movimentacao;
import imd.ufrn.br.cashbooks.model.Usuario;
import imd.ufrn.br.cashbooks.model.enums.MovimentacaoStatus;
import imd.ufrn.br.cashbooks.repository.MovimentacaoRepository;
import imd.ufrn.br.cashbooks.repository.UsuarioRepository;
import imd.ufrn.br.cashbooks.service.exceptions.DatabaseException;
import imd.ufrn.br.cashbooks.service.exceptions.ResourceNotFoundException;
import imd.ufrn.br.cashbooks.service.exceptions.ValidationException;

@Service
public class MovimentacaoService {
	
	@Autowired
	private MovimentacaoRepository repository;
	
	@Autowired
	private ClienteService serviceCliente;
	
	
	@Autowired
	private UsuarioRepository proprietarioRepository;
	
	private ICategorizarAutomaticamente categoriaStrategy;
	
	private IGerarRelatorio relatorioStategy;
	
	private IRestricoesComprasPrazo prazoStrategy;
	
	public void setStrategy (ICategorizarAutomaticamente strategy) {
		this.categoriaStrategy = strategy;
	}
	
	public void setRelatorioStrategy(IGerarRelatorio strategy) {
		this.relatorioStategy = strategy;
	}
	
	public void setPrazoStrategy(IRestricoesComprasPrazo strategy) {
		this.prazoStrategy = strategy;
	}
	
	public List<Movimentacao> findAll(){
		return repository.findAll();
	}
	
	public Movimentacao findById(Long id) {
		Optional<Movimentacao> obj = repository.findById(id);

        return obj.orElseThrow(() -> new ResourceNotFoundException(id));
	}
	
	public Movimentacao insert(Movimentacao obj) {		
		Optional<Usuario> op = proprietarioRepository.findById(1L);
		Usuario prop = op.get();
		
		ValidationException exception = new ValidationException("errors");
		
		if(obj.getCliente().getId() == null) {//Movimentação sem cliente
			obj.setCliente(null);
		}
		
		if(obj.getDataCobranca() == null) {
			exception.addError("dataCobranca", "campo vazio");
		}
		
		if(obj.getDataMovimentacao() == null) {
			exception.addError("dataMovimentacao", "campo vazio");
		}
		
		if(obj.getDataCobranca() != null && obj.getDataMovimentacao() != null) {
			if(obj.getDataCobranca().isBefore(obj.getDataMovimentacao())) {
				exception.addError("datas", "A data de cobranca não pode ser anterior a data de movimentacao");
			}
		}
		
		if(obj.getDescricao() == null) {
			exception.addError("descricao", "campo vazio");
		}
		
		if(obj.getStatus() == null) {
			exception.addError("status", "campo vazio");
		}
		
		if(obj.getValor() == null) {
			exception.addError("valor", "campo vazio");
		}
		
		if(exception.getErrors().size() > 0) {
			throw exception;
		}
		
		
		if(obj.getStatus() == MovimentacaoStatus.ENTRADA) {
			prop.setSaldo(prop.getSaldo() + obj.getValor());
        } else if(obj.getStatus() == MovimentacaoStatus.SAIDA){
			prop.setSaldo(prop.getSaldo() - obj.getValor());
        }
		
		Movimentacao mov = repository.save(obj);
		
        return mov;
    }
	
	public void delete(Long id) {
        try {
            repository.deleteById(id);
        } catch (EmptyResultDataAccessException e) {
        	throw new ResourceNotFoundException(id);
        } catch (DataIntegrityViolationException e) {
            throw new DatabaseException(e.getMessage());
        }
    }
	
	public Movimentacao update(Long id, Movimentacao obj) {
		Movimentacao entity = null;
		try {
            entity = repository.getById(id);
            updateData(entity, obj);
            return repository.save(entity);
        } catch(EntityNotFoundException e) {
            throw new ResourceNotFoundException(id);
        }
    }

	private void updateData(Movimentacao entity, Movimentacao obj) {
		//TODO
		entity.setDataCobranca(obj.getDataCobranca());
		entity.setValor(obj.getValor());
		entity.setDescricao(obj.getDescricao());
		entity.setCliente(obj.getCliente());
		entity.setStatus(obj.getStatus());
		
	}
	
	public Double getBalancoDiario() {	
		return getBalancoDoDia(LocalDate.now());
	}

	public List<Double> getBalancoRetroativamente(int diasAVer) {
		List<Double> saldos = new ArrayList<>();

		for(LocalDate date = LocalDate.now().minusDays(diasAVer); date.isBefore(LocalDate.now()) || date.isEqual(LocalDate.now()); date = date.plusDays(1)) {
			saldos.add(getBalancoDoDia(date));
		}
		
		return saldos;
	}
		
	public Double getBalancoDoDia(LocalDate data) {

		List<Movimentacao> movimentacoes = repository.findAllByDataMovimentacao(data);
		Double saldo = 0.0;
		
		for(Movimentacao mov : movimentacoes) {
			if(mov.getStatus() == MovimentacaoStatus.ENTRADA && mov.getDataMovimentacao().isEqual(mov.getDataCobranca()))
				saldo += mov.getValor();
	        else if (mov.getStatus() == MovimentacaoStatus.SAIDA && mov.getDataMovimentacao().isEqual(mov.getDataCobranca()))
	        	saldo -= mov.getValor();
		
		}
		
		return saldo;
	}
	
	public List<Movimentacao> getMovimentacoesFiadoHoje() {
		
		
		LocalDate dataLocal = LocalDate.now();
		

		List<Movimentacao> movimentacoes = repository.findAllByDataCobranca(dataLocal);
		List<Movimentacao> movimentacoesFiado = new ArrayList<Movimentacao>();
		
		for(Movimentacao mov : movimentacoes) {
			if(!mov.isPago()) {
				movimentacoesFiado.add(mov);
			}
		}
		
		return movimentacoesFiado;
	}
	
	public List<Movimentacao> getMovimentacoesFiado(){
		
		List<Movimentacao> movimentacoes = repository.findAll();
		List<Movimentacao> movimentacoesFiado = new ArrayList<Movimentacao>();
		
		for(Movimentacao mov : movimentacoes) {
			if(!mov.isPago()) {
				movimentacoesFiado.add(mov);
			}
		}
		
		return movimentacoesFiado;
	}
	
	public List<Movimentacao>findAllByCliente(Cliente cliente){
		return repository.findAllByCliente(cliente);
	}
	
	public Movimentacao pagarMovimentacao(Long id) {
		Movimentacao mov = findById(id);
		
		mov.setPago(true);
		update(id, mov);
		
		return mov;
	}
	
	public List<Movimentacao> getDividas(){
		List<Movimentacao> movimentacoesSaida = repository.findAll();
		List<Movimentacao> movimentacoesDivida = new ArrayList();
		
		for(Movimentacao mov : movimentacoesSaida) {
			if((!mov.isPago()) && mov.getStatus() == MovimentacaoStatus.SAIDA) {
				movimentacoesDivida.add(mov);
			}
		}
		
		return movimentacoesDivida;
	}
	
}
