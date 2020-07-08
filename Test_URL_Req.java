import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONException;
import org.json.JSONObject;

import java.awt.*;
import javax.swing.*;
import java.awt.event.*;

class Gui implements ActionListener, ItemListener {
	String basefrom;
	String baseto;
	String usd;
	String country[] = { "CAD", "HKD", "ISK", "PHP", "DKK", "HUF", "CZK", "GBP", "RON", "SEK", "IDR", "INR", "BRL",
			"RUB", "HRK", "JPY", "THB", "CHF", "EUR", "MYR", "BGN", "TRY", "CNY", "NOK", "NZD", "ZAR", "USD", "MXN",
			"SGD", "AUD", "ILS", "KRW", "PLN" };
	JSONObject rates_object;
	JTextField input = new JTextField(10);
	JTextField output = new JTextField(10);
	Choice fromSelect = new Choice();
	Choice toSelect = new Choice();
	Database db = new Database();

	Gui() {

		JFrame frame = new JFrame("Converter");
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				super.windowClosing(e);
				// clossing database on exit
				try {
					db.conn.close();
					System.out.println("clossing connection :" + db.conn.isClosed());

				} catch (Exception err) {
					err.getStackTrace();

				}
			}
		});
		JPanel jp = new JPanel();

		frame.setSize(500, 300);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		frame.add(jp);
		JLabel ip = new JLabel("From :");
		JLabel to = new JLabel("To :");
		JLabel amt = new JLabel("Amount :");

		jp.add(ip);

		fromSelect.addItemListener(this);
		toSelect.addItemListener(this);
		input.addActionListener(this);

		fromSelect.add("Select");
		for (int i = 0; i < country.length; i++) {
			fromSelect.add(country[i]);
		}
		toSelect.add("Select");
		for (int i = 0; i < country.length; i++) {
			toSelect.add(country[i]);
		}

		jp.add(fromSelect);
		jp.add(amt);
		jp.add(input);

		fromSelect.select(0);
		toSelect.select(0);
		JButton jb = new JButton("Convert");
		jb.addActionListener(this);

		jp.add(to);
		jp.add(toSelect);
		jp.add(output);
		jp.add(jb);

		frame.setVisible(true);

	}

	public void itemStateChanged(ItemEvent ie) {
		basefrom = fromSelect.getSelectedItem();
		baseto = toSelect.getSelectedItem();

		System.out.println(basefrom);

	}

	public void actionPerformed(ActionEvent ae) {
		usd = input.getText();
		System.out.println(usd);
		float ip = Float.parseFloat(usd);
		HttpRequest req = new HttpRequest();
		rates_object = req._Get(basefrom);

		db.createTable();

		if (rates_object.isNull(baseto)) {
			Double rate = db.read(basefrom + baseto);
			System.out.println("jsonobject is  null  for " + basefrom + baseto);
			System.out.println(rate);
			if (rate == 0.0)
				output.setText("Connection error");
			else
				output.setText(Double.toString(ip * rate));

		} else {

			try {

				output.setText(Double.toString(ip * rates_object.getDouble(baseto)));

				for (int i = 0; i < country.length; i++) {
					db.insert(basefrom + country[i], rates_object.getDouble(country[i]));
				}

			} catch (JSONException e) {
				e.printStackTrace();
			}

		}

	}

}

class Test_URL_Req {
	public static void main(String[] args) {

		new Gui();

	}
}

/**
 * InnerTest_URL_Req
 */
class HttpRequest {

	JSONObject _Get(String basefrom) {
		try {
			String url = "https://api.exchangeratesapi.io/latest?base=" + basefrom;
			URL obj = new URL(url);
			HttpURLConnection con = (HttpURLConnection) obj.openConnection();
			int responseCode = con.getResponseCode();
			System.out.println("Sending 'GET' request to URL : " + url);
			System.out.println("Response Code : " + responseCode);
			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();
			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();
			JSONObject myresponse = new JSONObject(response.toString());
			JSONObject rates_object = new JSONObject(myresponse.getJSONObject("rates").toString());

			return rates_object;
		} catch (Exception e) {
			System.out.println(e);
			return new JSONObject();
		}

	}

}

class Database {
	/**
	 * Connect to a sample database
	 */
	public Connection conn;

	Database() {
		this.connect();
	}

	private void connect() {

		try {
			Class.forName("org.sqlite.JDBC");
		} catch (ClassNotFoundException e) {
			System.out.println(e);
		}
		try {
			// db parameters
			String url = "jdbc:sqlite:./currency_converter.db";
			// create a connection to the database
			this.conn = DriverManager.getConnection(url);

			System.out.println("Connection to SQLite has been established.");

		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}

	}

	public void insert(String exchange, Double rate) {
		String sql = "INSERT OR REPLACE INTO exchange_rates(exchange,rate) VALUES(?,?)";

		try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setString(1, exchange);
			pstmt.setDouble(2, rate);
			pstmt.executeUpdate();

		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
	}

	public void createTable() {

		String sql = "CREATE TABLE IF NOT EXISTS exchange_rates (exchange text PRIMARY KEY, rate double);";

		try (Statement stmt = conn.createStatement()) {
			stmt.execute(sql);

		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}

	}

	Double read(String key) {

		Double rate = 0.0;
		ResultSet rs;
		PreparedStatement pstmt = null;
		String sql = "SELECT * FROM exchange_rates WHERE exchange=?";

		try {
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, key);
			rs = pstmt.executeQuery();
			rate = rs.getDouble("rate");
			System.out.println("this is the rate : " + rate);
			return rate;

		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}

	}

}