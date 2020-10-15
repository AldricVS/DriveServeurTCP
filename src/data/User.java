package data;

/**
 * Class defining the user currently connected to the server
 * @author Aldric
 *
 */
public class User {

	private String name;
	private boolean isAdmin;
	
	public User(String name, boolean isAdmin) {
		this.name = name;
		this.isAdmin = isAdmin;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isAdmin() {
		return isAdmin;
	}

	public void setAdmin(boolean isAdmin) {
		this.isAdmin = isAdmin;
	}

	@Override
	public String toString() {
		return "User [name=" + name + ", isAdmin=" + isAdmin + "]";
	}
}
