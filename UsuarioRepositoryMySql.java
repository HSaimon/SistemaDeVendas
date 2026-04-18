package com.example.SistemaDeVendas.repositories;

import com.example.SistemaDeVendas.entities.Usuario;
import com.example.SistemaDeVendas.interfacies.IUsuario;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public class UsuarioRepositoryMySql implements IUsuario {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    public UsuarioRepositoryMySql(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Transactional
    @Override
    public void salvar(Usuario usuario) {
        this.entityManager.persist(usuario);
    }

    @Override
    public Usuario buscarPorId(int id) {
        return this.entityManager.find(Usuario.class, id);
    }

    @Override
    public List<Usuario> buscarTodos() {
        return entityManager
                .createQuery("SELECT u FROM Usuario u ORDER BY u.cpf", Usuario.class)
                .getResultList();
    }

    @Transactional
    @Override
    public void atualizar(int id, Usuario usuario) {
        Usuario usuarioInDB = this.entityManager.find(Usuario.class, id);
        if (usuarioInDB == null) {
            throw new EntityNotFoundException("Usuário não encontrado com id: " + id);
        }
        usuarioInDB.setCpf(usuario.getCpf());
        usuarioInDB.setSenha(usuario.getSenha());
        // CORRIGIDO: era usuarioInDB.getFuncionario() — copiava de si mesmo
        usuarioInDB.setFuncionario(usuario.getFuncionario());
        usuarioInDB.setEnabled(usuario.isEnabled());
        this.entityManager.merge(usuarioInDB);
    }

    @Transactional
    @Override
    public void deletar(int id) {
        Query query = entityManager.createQuery("DELETE FROM Usuario u WHERE u.id = :id");
        query.setParameter("id", id);
        query.executeUpdate();
    }

    @Transactional
    public void deleteAll() {
        Query query = entityManager.createQuery("DELETE FROM Usuario u");
        query.executeUpdate();
    }
}
