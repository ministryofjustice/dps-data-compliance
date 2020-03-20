package uk.gov.justice.hmpps.datacompliance.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class UserSecurityUtils {

	@SuppressWarnings("rawtypes")
	public Optional<String> getCurrentUsername() {

		return getUserPrincipal().map(userPrincipal -> {

			if (userPrincipal instanceof String) {
				return (String) userPrincipal;
			} else if (userPrincipal instanceof UserDetails) {
				return ((UserDetails) userPrincipal).getUsername();
			} else if (userPrincipal instanceof Map) {
				return (String) ((Map) userPrincipal).get("username");
			}
			return null;

		});
    }

	private Optional<Object> getUserPrincipal() {
		return getAuthentication().map(Authentication::getPrincipal);
	}

	private Optional<Authentication> getAuthentication() {
		return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication());
	}
}
