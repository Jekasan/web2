package ru.fisunov.http.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ItemsWebApplication implements MyWebApplication {
    private String name;
    private List<Item> items;

    Connection connection;
    private static final Logger logger = LogManager.getLogger(CalculatorWebApplication.class);

    public ItemsWebApplication() throws SQLException, ClassNotFoundException {
        this.name = "Items Web Application";
        items = new ArrayList<>();

        connection = DriverManager.getConnection("jdbc:sqlite:server\\items3.db");
    }

    private void getItemsFromDB(List<Item> items, Connection con) {
        String sqlLoad = "SELECT ID, TITLE FROM ITEMS";
        items.clear();
        try (Statement statement = con.createStatement();
             ResultSet rs = statement.executeQuery(sqlLoad)) {
            while (rs.next()) {
                Item item = new Item(
                        rs.getLong("ID"),
                        rs.getString("TITLE"));
                items.add(item);
            }
        } catch (SQLException ex) {
            if (ex.getMessage().contains("no such table: ITEMS")) {
                logger.debug("no such table: ITEMS");
                String sqlCreate = "CREATE TABLE ITEMS ( ID INTEGER, TITLE TEXT(100) );";
                try (Statement statement = con.createStatement() ) {
                    statement.execute("CREATE TABLE ITEMS ( ID INTEGER, TITLE TEXT(100) );");
                    logger.info("ITEMS create");
                } catch (SQLException ex2) {
                    logger.error(ex2.getMessage());
                    throw new RuntimeException(ex2.getMessage());
                }
            }
            else {
                logger.error(ex.getMessage());
                throw new RuntimeException(ex.getMessage());
            }
        }
    }

    private void saveItemToDB(Item item, Connection con) {
        String sqlInsert = "INSERT INTO ITEMS( ID, TITLE ) VALUES ( ?, ? ) ";
        try (PreparedStatement ps = con.prepareStatement(sqlInsert)) {
            ps.setLong(1, item.getId());
            ps.setString(2, item.getTitle());
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException(ex.getMessage());
        }
    }

    @Override
    public void execute(Request request, OutputStream output) throws IOException {
        logger.info(request.getMethod().toString());
        if (request.getMethod() == Method.GET) {
            getItemsFromDB(items, connection);

            Gson gson = new GsonBuilder()
                    .setPrettyPrinting()
                    .create();
            String json = gson.toJson(items);

            output.write(("" +
                    "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: application/json\r\n" +
                    "Access-Control-Allow-Origin: *\r\n" +
                    "\r\n" +
                    json
            ).getBytes(StandardCharsets.UTF_8));
        }
        if (request.getMethod() == Method.POST) {
            String json = request.getBody();
            Gson gson = new Gson();
            Item item = gson.fromJson(json, Item.class);

            saveItemToDB(item, connection);

            output.write(("" +
                    "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: text/html\r\n" +
                    "\r\n"
            ).getBytes(StandardCharsets.UTF_8));
        }
    }
}