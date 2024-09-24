package umm3601.todo;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.regex;

import org.bson.UuidRepresentation;
import org.bson.types.ObjectId;
import org.mongojack.JacksonMongoCollection;

import com.mongodb.client.MongoDatabase;

import io.javalin.Javalin;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.http.NotFoundResponse;
import umm3601.Controller;

public class TodoController implements Controller{

  private static final String API_TODOS = "/api/todos";
  private static final String API_TODO_BY_ID = "/api/todos/{id}";
  static final String CATEGORY_KEY = "category";
  static final String STATUS_KEY = "status";

  static final String SORT_ORDER_KEY = "sortorder";

  // from UserController.java - possibly not use
  // private static final int REASONABLE_AGE_LIMIT = 150;
  // private static final String ROLE_REGEX = "^(admin|editor|viewer)$";
  // public static final String EMAIL_REGEX = "^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+$";

  private final JacksonMongoCollection<Todo> todoCollection;


  public TodoController(MongoDatabase database) {
    todoCollection = JacksonMongoCollection.builder().build(
        database,
        "todos",
        Todo.class,
        UuidRepresentation.STANDARD);
  }

  public void getTodo(Context ctx) { //gets singular
    String id = ctx.pathParam("id");
    Todo todo;

    try {
      todo = todoCollection.find(eq("_id", new ObjectId(id))).first();
    } catch (IllegalArgumentException e) {
      throw new BadRequestResponse("The requested todo id wasn't a legal Mongo Object ID.");
    }
    if (todo == null) {
      throw new NotFoundResponse("The requested todo was not found");
    } else {
      ctx.json(todo);
      ctx.status(HttpStatus.OK);
    }
  }

  public void getTodos(Context ctx) { //gets more than one todo
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'getTodos'");
  }

  public void addRoutes(Javalin mockServer) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'addRoutes'");
  }

}
