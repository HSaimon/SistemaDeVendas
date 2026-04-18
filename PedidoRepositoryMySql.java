package com.example.SistemaDeVendas.repositories;

import com.example.SistemaDeVendas.entities.Pedido;
import com.example.SistemaDeVendas.interfacies.IPedido;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public class PedidoRepositoryMySql implements IPedido {

    @PersistenceContext
    private final EntityManager entityManager;

    @Autowired
    public PedidoRepositoryMySql(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Transactional
    @Override
    public void salvar(Pedido pedido) {
        this.entityManager.persist(pedido);
    }

    @Override
    public Pedido buscarPorId(int id) {
        return this.entityManager.find(Pedido.class, id);
    }

    @Override
    public List<Pedido> buscarTodos() {
        return entityManager
                .createQuery("SELECT p FROM Pedido p ORDER BY p.dataPedido DESC", Pedido.class)
                .getResultList();
    }

    @Transactional
    @Override
    public void atualizar(int id, Pedido pedido) {
        Pedido pedidoInDB = this.entityManager.find(Pedido.class, id);
        if (pedidoInDB != null) {
            pedidoInDB.setDataPedido(pedido.getDataPedido());
            pedidoInDB.setIdCliente(pedido.getIdCliente());
            pedidoInDB.setValorTotal(pedido.getValorTotal());
            pedidoInDB.setIdUsuario(pedido.getIdUsuario());
            // CORRIGIDO: status e descontoFidelidade agora são persistidos
            pedidoInDB.setStatus(pedido.getStatus());
            pedidoInDB.setDescontoFidelidade(pedido.getDescontoFidelidade());
            this.entityManager.merge(pedidoInDB);
        }
    }

    @Transactional
    @Override
    public void deletar(int id) {
        // CORRIGIDO: era "delete from Produto" — apagava produto em vez de pedido
        Pedido pedido = this.entityManager.find(Pedido.class, id);
        if (pedido != null) {
            this.entityManager.remove(pedido);
        }
    }
}
