package umm3601.todo;

import static com.mongodb.client.model.Filters.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.eclipse.jdt.internal.compiler.ast.InstanceOfExpression;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.exceptions.misusing.InjectMocksException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.mongodb.MongoClientSettings;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import io.javalin.Javalin;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.http.NotFoundResponse;
import io.javalin.json.JavalinJackson;
import io.javalin.validation.BodyValidator;
import io.javalin.validation.Validation;
import io.javalin.validation.ValidationError;
import io.javalin.validation.ValidationException;
import io.javalin.validation.Validator;

@SuppressWarnings({ "MagicNumber" })
class TodoControllerSpec {
    private TodoController todoController;
    private ObjectId todoId;
    private static MongoClient mongoClient;
    private static MongoDatabase db;
    private static JavalinJackson javalinJackson = new JavalinJackson();

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
            MongoClientSettings.builder().applyToClusterSettings(builder -> builder.hosts(Arrays.asList(new ServerAddress(mongoAddr)))).build());
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
            .append("owner", "Brendan")
            .append("category", "video games")
            .append("body", "Play rocket league.")
            .append("status", true));
        testTodos.add(
            new Document()
            .append("owner", "Brendan")
            .append("category", "software design")
            .append("body", "Make rocket league.")
            .append("status", true));
        testTodos.add(
            new Document()
            .append("owner", "Samantha")
            .append("category", "software design")
            .append("body", "make tests, import all todos")
            .append("status", true));
        testTodos.add(
            new Document()
            .append("owner", "Brendantha")
            .append("category", "software games")
            .append("body", "play todo.service.ts")
            .append("status", false));

        todoId = new ObjectId();
            Document testTodoId = new Document()
            .append("owner", "Samdan")
            .append("category", "video games")
            .append("body", "Make a fnaf 2 playthrough")
            .append("status", true)
            .append("_id", todoId);

        todoDocuments.insertMany(testTodos);
        todoDocuments.insertOne(testTodoId);

        todoController = new TodoController(db);
    }

    @Test
    public void canBuildController() throws IOException {
        Javalin mockServer = Mockito.mock(Javalin.class);
        todoController.addRoutes(mockServer);
        verify(mockServer, Mockito.atLeast(2)).get(any(), any());
    }

    @Test
    void canGetAllTodos() throws IOException {
        when(ctx.queryParamMap()).thenReturn(Collections.emptyMap());
        todoController.getTodos(ctx);
        verify(ctx).json(todoArrayListCaptor.capture());
        verify(ctx).status(HttpStatus.OK);
        assertEquals(db.getCollection("todos").countDocuments(), todoArrayListCaptor.getValue().size());
    }

    @Test
    void canGetTodosWithCategoryVideoGames() throws IOException {
        String targetCategory = "video games";
        Map<String, List<String>> queryParams = new HashMap<>();

        queryParams.put(TodoController.CATEGORY_KEY, Arrays.asList(new String[] {targetCategory}));
        when(ctx.queryParamMap()).thenReturn(queryParams);
        when(ctx.queryParam(TodoController.CATEGORY_KEY)).thenReturn(targetCategory);

        Validation validation = new Validation();
        Validator<String> validator = validation.validator(TodoController.CATEGORY_KEY, String.class, targetCategory);

        when(ctx.queryParamAsClass(TodoController.CATEGORY_KEY, String.class)).thenReturn(validator);

        todoController.getTodos(ctx);

        verify(ctx).json(todoArrayListCaptor.capture());
        verify(ctx).status(HttpStatus.OK);

        assertEquals(2, todoArrayListCaptor.getValue().size());

        for (Todo todo : todoArrayListCaptor.getValue()) {
            assertEquals(targetCategory, todo.category);
        }

        List<String> owners = todoArrayListCaptor.getValue().stream().map(todo -> todo.owner).collect(Collectors.toList());
        assertTrue(owners.contains("Brendan"));
        assertTrue(owners.contains("Samdan"));
    }
    @Test
    void canGetTodosWithOwner() throws IOException {
        String targetOwner = "Brendan";

        Map<String, List<String>> queryParams = new HashMap<>();

        queryParams.put(TodoController.OWNER_KEY, Arrays.asList(new String[] {targetOwner}));
        when(ctx.queryParamMap()).thenReturn(queryParams);
        when(ctx.queryParam(TodoController.OWNER_KEY)).thenReturn(targetOwner);

        Validation validation = new Validation();
        Validator<String> validator = validation.validator(TodoController.OWNER_KEY, String.class, targetOwner);

        when(ctx.queryParamAsClass(TodoController.CATEGORY_KEY, String.class)).thenReturn(validator);

        todoController.getTodos(ctx);

        verify(ctx).json(todoArrayListCaptor.capture());
        verify(ctx).status(HttpStatus.OK);

        assertEquals(3, todoArrayListCaptor.getValue().size());

        for (Todo todo : todoArrayListCaptor.getValue()) {
            assertTrue(todo.owner.contains("Brendan"));
        }
    }

    @Test
    public void canGetString() {

        String targetOwner = "Samantha";
        String targetString = "make tests, import all todos";

        Map<String, List<String>> queryParams = new HashMap<>();

        queryParams.put(TodoController.OWNER_KEY, Arrays.asList(new String[] {targetOwner}));
        when(ctx.queryParamMap()).thenReturn(queryParams);
        when(ctx.queryParam(TodoController.OWNER_KEY)).thenReturn(targetOwner);

        Validation validation = new Validation();
        Validator<String> validator = validation.validator(TodoController.OWNER_KEY, String.class, targetOwner);

        when(ctx.queryParamAsClass(TodoController.CATEGORY_KEY, String.class)).thenReturn(validator);

        todoController.getTodos(ctx);

        verify(ctx).json(todoArrayListCaptor.capture());
        verify(ctx).status(HttpStatus.OK);

        assertEquals(1, todoArrayListCaptor.getValue().size());

        for (Todo todo : todoArrayListCaptor.getValue()) {
            assertTrue(todo.toString().contains(targetString));
        }
    }

    @Test
void canGetTodoById() throws IOException {
    // Given
    ObjectId todoId = new ObjectId();
    Todo expectedTodo = new Todo();
    expectedTodo._id = todoId.toHexString(); // Assuming _id is a public field or has a setter
    expectedTodo.owner = "Alice";
    expectedTodo.category = "chores";
    expectedTodo.body = "Clean the house.";

    // Insert the todo into the database
    db.getCollection("todos").insertOne(new Document("_id", todoId)
        .append("owner", expectedTodo.owner)
        .append("category", expectedTodo.category)
        .append("body", expectedTodo.body));

    // Mock the context
    when(ctx.pathParam("id")).thenReturn(todoId.toHexString());

    // Call the method under test
    todoController.getTodo(ctx);

    // Verify the interactions with the context
    verify(ctx).json(expectedTodo);
    verify(ctx).status(HttpStatus.OK);
}

@Test
void cannotGetTodoWithInvalidId() {
    // Given
    String invalidId = "invalid-id";

    // Mock the context to return an invalid ID
    when(ctx.pathParam("id")).thenReturn(invalidId);

    // When / Then
    assertThrows(BadRequestResponse.class, () -> {
        todoController.getTodo(ctx);
    });
}

@Test
void cannotGetTodoThatDoesNotExist() {
    // Given
    ObjectId nonExistentId = new ObjectId(); // Generate a new ID that does not exist

    // Mock the context to return the non-existent ID as a String
    when(ctx.pathParam("id")).thenReturn(nonExistentId.toHexString());

    // When / Then
    assertThrows(NotFoundResponse.class, () -> {
        todoController.getTodo(ctx);
    });
}


@Test
void canGetTodosWithBody() throws IOException {
    String targetBody = "Make rocket league."; // Example body content

    Map<String, List<String>> queryParams = new HashMap<>();
    queryParams.put(TodoController.BODY_KEY, Arrays.asList(targetBody));

    when(ctx.queryParamMap()).thenReturn(queryParams);
    when(ctx.queryParam(TodoController.BODY_KEY)).thenReturn(targetBody);

    Validation validation = new Validation();
    Validator<String> validator = validation.validator(TodoController.BODY_KEY, String.class, targetBody);
    when(ctx.queryParamAsClass(TodoController.BODY_KEY, String.class)).thenReturn(validator);

    // Call the method under test
    todoController.getTodos(ctx);

    // Verify the context interactions
    verify(ctx).json(todoArrayListCaptor.capture());
    verify(ctx).status(HttpStatus.OK);

    // Check the captured todos
    List<Todo> todosCaptured = todoArrayListCaptor.getValue();
    assertEquals(1, todosCaptured.size()); // Adjust based on your setup

    // Verify the body of the captured todo
    for (Todo todo : todosCaptured) {
        assertTrue(todo.body.contains(targetBody));
    }
}

@Test
void canGetTodosWithStatus() throws IOException {
    boolean targetStatus = true; // Example status

    Map<String, List<String>> queryParams = new HashMap<>();
    queryParams.put(TodoController.STATUS_KEY, Arrays.asList(String.valueOf(targetStatus))); // Convert boolean to String

    when(ctx.queryParamMap()).thenReturn(queryParams);
    when(ctx.queryParam(TodoController.STATUS_KEY)).thenReturn(String.valueOf(targetStatus));

    Validation validation = new Validation();
    Validator<String> validator = validation.validator(TodoController.STATUS_KEY, String.class, String.valueOf(targetStatus));
    when(ctx.queryParamAsClass(TodoController.STATUS_KEY, String.class)).thenReturn(validator);

    // Call the method under test
    todoController.getTodos(ctx);

    // Verify the context interactions
    verify(ctx).json(todoArrayListCaptor.capture());
    verify(ctx).status(HttpStatus.OK);

    // Check the captured todos
    List<Todo> todosCaptured = todoArrayListCaptor.getValue();
    // Adjust the expected size based on your test setup
    assertEquals(0, todosCaptured.size()); // Change this to the expected count for the given status

    // Verify that each captured todo has the expected status
    for (Todo todo : todosCaptured) {
        assertEquals(targetStatus, todo.status); // Check if the status matches the expected boolean value
    }
}



}
