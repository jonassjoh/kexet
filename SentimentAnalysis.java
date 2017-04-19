import java.sql.SQLException;
import java.util.ArrayList;

public class SentimentAnalysis {

	private Database db;
	
	public SentimentAnalysis() {
		db = new Database();
		if(!db.init()) {
			System.out.println("Could not initiate database, quitting");
			System.exit(0);
		}
		
		try {
			ArrayList<User> users = db.getUsers();
			db.getReviewsForAll(users);
			ReviewTokenizer.tokenizeAllTitles(users);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
