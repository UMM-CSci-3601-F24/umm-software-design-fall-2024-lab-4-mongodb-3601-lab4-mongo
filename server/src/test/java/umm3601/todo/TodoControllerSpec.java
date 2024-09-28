package umm3601.todo;

//import static com.mongodb.client.model.Filters.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertNotEquals;
//import static org.junit.jupiter.api.Assertions.assertNotNull;
//import static org.junit.jupiter.api.Assertions.assertThrows;
//import static org.junit.jupiter.api.Assertions.assertTrue;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
//import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
//import java.util.stream.Collectors;

import org.bson.Document;
//import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
//import org.mockito.ArgumentMatcher;
import org.mockito.Captor;
import org.mockito.Mock;
//import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.databind.JsonMappingException;
import com.mongodb.MongoClientSettings;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

//import io.javalin.Javalin;
//import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
//import io.javalin.http.NotFoundResponse;
//import io.javalin.json.JavalinJackson;
//import io.javalin.validation.BodyValidator;
import io.javalin.validation.Validation;
//import io.javalin.validation.ValidationError;
//import io.javalin.validation.ValidationException;
import io.javalin.validation.Validator;

@SuppressWarnings({ "MagicNumber" })
public class TodoControllerSpec {
  private TodoController todoController;

  //private ObjectId samsId;
  private static MongoClient mongoClient;
  private static MongoDatabase db;

  //private static JavalinJackson javalinJackson = new JavalinJackson();

  @Mock
  private Context ctx;

  @Captor
  private ArgumentCaptor<ArrayList<Todo>> todoArrayListCaptor;

  @Captor
  private ArgumentCaptor<Todo> todoCaptor;

  @Captor
  private ArgumentCaptor<Map<String, String>> mapCaptor;

  @BeforeAll
  static void setupAll() {
    String mongoAddr = System.getenv().getOrDefault("MONGO_ADDR", "localhost");

    mongoClient = MongoClients.create(
        MongoClientSettings.builder()
            .applyToClusterSettings(builder -> builder.hosts(Arrays.asList(new ServerAddress(mongoAddr))))
            .build());
    db = mongoClient.getDatabase("test");
  }

  @AfterAll
  static void teardown() {
    db.drop();
    mongoClient.close();
  }

  @BeforeEach
  void setupEach() throws IOException {
    MockitoAnnotations.openMocks(this);
    MongoCollection<Document> todoDocuments = db.getCollection("todos");
    todoDocuments.drop();
    List<Document> testTodos = new ArrayList<>();
    testTodos.add(
      new Document()
          .append("owner", "Blanche")
          .append("status", false)
          .append("body", "In sunt ex non tempor cillum commodo amet incididunt anim qui"
          + " commodo quis. Cillum non labore ex sint esse.")
          .append("category", "software design"));
    // samsId = new ObjectId();
    // Document sam = new Document()
    //     .append("_id", "58af3a600343927e48e8720f")
    //     .append("owner", "Blanche");
      testTodos.add(
        new Document()
            .append("owner", "Fry")
            .append("status", false)
            .append("body", "Ipsum esse est ullamco magna tempor anim laborum non officia deserunt "
            + "veniam commodo. Aute minim incididunt ex commodo.")
            .append("category", "video games")
      );
      testTodos.add(
        new Document()
            .append("owner", "Fry")
            .append("status", true)
            .append("body", "Ullamco irure laborum magna dolor non. Anim occaecat adipisicing cillum eu magna in.")
            .append("category", "homework")
      );


    todoDocuments.insertMany(testTodos);
    // todoDocuments.insertOne(sam);

    todoController = new TodoController(db);
  }

  // @Test
  // public void canBuildController() throws IOException {
  //   Javalin mockServer = Mockito.mock(Javalin.class);
  //   todoController.addRoutes(mockServer);

  //   verify(mockServer, Mockito.atLeast(2)).get(any(), any());
  // }

  @Test
  void canGetAllTodos() throws IOException {
    when(ctx.queryParamMap()).thenReturn(Collections.emptyMap());

    todoController.getTodos(ctx);

    verify(ctx).json(todoArrayListCaptor.capture());
    verify(ctx).status(HttpStatus.OK);

    assertEquals(db.getCollection("todos").countDocuments(), todoArrayListCaptor.getValue().size());
  }

@Test
  void getTodosByOwner() throws IOException {
    Map<String, List<String>> queryParams = new HashMap<>();
    String ownerString = "Fry";
    queryParams.put(TodoController.OWNER_KEY, Arrays.asList(new String[] {ownerString}));
    when(ctx.queryParamMap()).thenReturn(queryParams);
    when(ctx.queryParam(TodoController.OWNER_KEY)).thenReturn(ownerString);

    todoController.getTodos(ctx);

    verify(ctx).json(todoArrayListCaptor.capture());
    verify(ctx).status(HttpStatus.OK);
    assertEquals(2, todoArrayListCaptor.getValue().size());
    for (Todo todo : todoArrayListCaptor.getValue()) {
      assertEquals("Fry", todo.owner);
    }
  }

  @Test
  void getTodosByOwner2() throws IOException {
    Map<String, List<String>> queryParams = new HashMap<>();
    String ownerString = "Owen";
    queryParams.put(TodoController.OWNER_KEY, Arrays.asList(new String[] {ownerString}));
    when(ctx.queryParamMap()).thenReturn(queryParams);
    when(ctx.queryParam(TodoController.OWNER_KEY)).thenReturn(ownerString);

    todoController.getTodos(ctx);

    verify(ctx).json(todoArrayListCaptor.capture());
    verify(ctx).status(HttpStatus.OK);
    assertEquals(0, todoArrayListCaptor.getValue().size());
    for (Todo todo : todoArrayListCaptor.getValue()) {
      assertEquals("", todo.owner);
    }
  }

  @Test
  void getTodosByOwnerCAPS() throws IOException {
    Map<String, List<String>> queryParams = new HashMap<>();
    String ownerString = "frY";
    queryParams.put(TodoController.OWNER_KEY, Arrays.asList(new String[] {ownerString}));
    when(ctx.queryParamMap()).thenReturn(queryParams);
    when(ctx.queryParam(TodoController.OWNER_KEY)).thenReturn(ownerString);

    todoController.getTodos(ctx);

    verify(ctx).json(todoArrayListCaptor.capture());
    verify(ctx).status(HttpStatus.OK);
    assertEquals(2, todoArrayListCaptor.getValue().size());
    for (Todo todo : todoArrayListCaptor.getValue()) {
      assertEquals("Fry", todo.owner);
    }
  }

  @Test
  void getTodosByStatus() throws IOException {
    Map<String, List<String>> queryParams = new HashMap<>();
    String statusString = "true";
    queryParams.put(TodoController.STATUS_KEY, Arrays.asList(new String[] {statusString}));
    when(ctx.queryParamMap()).thenReturn(queryParams);
    when(ctx.queryParam(TodoController.STATUS_KEY)).thenReturn(statusString);

    Validation validation = new Validation();
    Validator<Boolean> validator = validation.validator(TodoController.STATUS_KEY, Boolean.class, statusString);

    when(ctx.queryParamAsClass(TodoController.STATUS_KEY, Boolean.class)).thenReturn(validator);

    todoController.getTodos(ctx);

    verify(ctx).json(todoArrayListCaptor.capture());
    verify(ctx).status(HttpStatus.OK);
    assertEquals(1, todoArrayListCaptor.getValue().size());
  }

  @Test
  void getTodosByStatus2() throws IOException {
    Map<String, List<String>> queryParams = new HashMap<>();
    String statusString = "false";
    queryParams.put(TodoController.STATUS_KEY, Arrays.asList(new String[] {statusString}));
    when(ctx.queryParamMap()).thenReturn(queryParams);
    when(ctx.queryParam(TodoController.STATUS_KEY)).thenReturn(statusString);

    Validation validation = new Validation();
    Validator<Boolean> validator = validation.validator(TodoController.STATUS_KEY, Boolean.class, statusString);

    when(ctx.queryParamAsClass(TodoController.STATUS_KEY, Boolean.class)).thenReturn(validator);

    todoController.getTodos(ctx);

    verify(ctx).json(todoArrayListCaptor.capture());
    verify(ctx).status(HttpStatus.OK);
    assertEquals(2, todoArrayListCaptor.getValue().size());
  }

  @Test
  void getTodosByStatus3() throws IOException {
    Map<String, List<String>> queryParams = new HashMap<>();
    String statusString = "TRUE";
    queryParams.put(TodoController.STATUS_KEY, Arrays.asList(new String[] {statusString}));
    when(ctx.queryParamMap()).thenReturn(queryParams);
    when(ctx.queryParam(TodoController.STATUS_KEY)).thenReturn(statusString);

    Validation validation = new Validation();
    Validator<Boolean> validator = validation.validator(TodoController.STATUS_KEY, Boolean.class, statusString);

    when(ctx.queryParamAsClass(TodoController.STATUS_KEY, Boolean.class)).thenReturn(validator);

    todoController.getTodos(ctx);

    verify(ctx).json(todoArrayListCaptor.capture());
    verify(ctx).status(HttpStatus.OK);
    assertEquals(1, todoArrayListCaptor.getValue().size());
  }
}
