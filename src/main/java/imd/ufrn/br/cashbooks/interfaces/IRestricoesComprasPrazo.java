package imd.ufrn.br.cashbooks.interfaces;

import imd.ufrn.br.cashbooks.model.Movimentacao;

public interface IRestricoesComprasPrazo {
	public void calcularDataLimite(Movimentacao mov);
	public void validarPrazo(Movimentacao mov);
	public int getLimite();
}
