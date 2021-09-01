package imd.ufrn.br.cashbooks.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import imd.ufrn.br.cashbooks.model.Usuario;
import imd.ufrn.br.cashbooks.model.UsuarioPrincipal;
import imd.ufrn.br.cashbooks.repository.UsuarioRepository;

public class UsuarioPrincipalService implements UserDetailsService{
	
	@Autowired
	private UsuarioRepository usuarioRepository;
	
	private BCryptPasswordEncoder bcryptEncoder;

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		Optional<Usuario> opt = usuarioRepository.findByUsername(username);
		Usuario usuario = null;
		if(opt.isPresent()) {
			usuario = opt.get();
		}
		if(usuario==null) {
			throw new UsernameNotFoundException(username);
		}
		return new UsuarioPrincipal(usuario);
	}
	
	public Usuario save(Usuario usuario) {
        usuario.setSenha(bcryptEncoder.encode(usuario.getSenha()));
        return usuarioRepository.save(usuario);
    }

}
