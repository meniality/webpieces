package org.webpieces.plugins.hibernate.app;

import static org.webpieces.plugins.hibernate.app.HibernateRouteId.ADD_USER_PAGE;
import static org.webpieces.plugins.hibernate.app.HibernateRouteId.EDIT_USER_PAGE;

import java.util.List;

import javax.inject.Singleton;
import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.webpieces.ctx.api.Current;
import org.webpieces.plugins.hibernate.Em;
import org.webpieces.plugins.hibernate.app.dbo.LevelEducation;
import org.webpieces.plugins.hibernate.app.dbo.UserTestDbo;
import org.webpieces.router.api.actions.Actions;
import org.webpieces.router.api.actions.FlashAndRedirect;
import org.webpieces.router.api.actions.Redirect;
import org.webpieces.router.api.actions.Render;
import org.webpieces.util.logging.Logger;
import org.webpieces.util.logging.LoggerFactory;

@Singleton
public class CrudTestController {
	
	private static final Logger log = LoggerFactory.getLogger(HibernateAsyncController.class);
	
	public Render userList() {
		EntityManager mgr = Em.get();
		Query query = mgr.createNamedQuery("findAllUsers");
		@SuppressWarnings("unchecked")
		List<UserTestDbo> users = query.getResultList();
		return Actions.renderThis("users", users);
	}
	
	public Render userAddEdit(Integer id) {
		if(id == null) {
			return Actions.renderThis("entity", new UserTestDbo(),
					"levels", LevelEducation.values());
		}
		
		EntityManager mgr = Em.get();
		UserTestDbo user = mgr.find(UserTestDbo.class, id);
		return Actions.renderThis(
				"entity", user,
				"levels", LevelEducation.values());
	}
	
	public Redirect postSaveUser(UserTestDbo user) {
		if(user.getPassword().length() < 4) {
			Current.validation().addError("password", "Value is too short");
		}
		
		if(Current.validation().hasErrors()) {
			log.info("page has errors");
			FlashAndRedirect redirect = new FlashAndRedirect(Current.getContext(), "Errors in form below");
			redirect.setIdFieldAndValue("id", user.getId());
			redirect.setPageAndRouteArguments("other", "value", "key3", "value3");
			Actions.redirectFlashAll(ADD_USER_PAGE, EDIT_USER_PAGE, redirect);
		}
		
		Current.flash().setMessage("User successfully saved");
		Em.get().merge(user);
        Em.get().flush();
        
		return Actions.redirect(HibernateRouteId.LIST_USERS);
	}
	
	public Render confirmDeleteUser(Integer id) {
		UserTestDbo user = Em.get().find(UserTestDbo.class, id);
		return Actions.renderThis("user", user);
	}
	
    public Redirect postDeleteUser(Integer id) {
    	UserTestDbo user = Em.get().getReference(UserTestDbo.class, id);
    	Em.get().remove(user);
    	Em.get().flush();
    	
    	Current.flash().setMessage("User "+user.getFirstName()+" "+user.getLastName()+" was deleted");
    	return Actions.redirect(HibernateRouteId.LIST_USERS);
    }
	
}
