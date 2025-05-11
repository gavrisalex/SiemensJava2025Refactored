package com.siemens.internship;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class InternshipApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private ItemService itemService;

	@Autowired
	private ObjectMapper objectMapper;

	private Validator validator;
	private Item testItem;

	@BeforeEach
	void setUp() {
		ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
		validator = factory.getValidator();

		testItem = new Item();
		testItem.setId(1L);
		testItem.setName("Test Item");
		testItem.setEmail("test@example.com");
		testItem.setStatus("NEW");
	}

	// Validation Tests
	@Test
	void whenAllFieldsValid_thenNoViolations() {
		var violations = validator.validate(testItem);
		assertTrue(violations.isEmpty());
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"invalid-email",
			"@nodomain.com",
			"no-at-sign.com",
			"spaces@ domain.com",
			"@.com",
			"user@domain"
	})
	void whenInvalidEmail_thenValidationFails(String email) {
		testItem.setEmail(email);
		var violations = validator.validate(testItem);
		assertFalse(violations.isEmpty());
		assertTrue(violations.stream()
				.anyMatch(v -> v.getPropertyPath().toString().equals("email")));
	}

	// Controller Tests
	@Test
	void getAllItems_ReturnsItems() throws Exception {
		when(itemService.findAll()).thenReturn(Arrays.asList(testItem));

		mockMvc.perform(get("/api/items"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(1))
				.andExpect(jsonPath("$[0].name").value("Test Item"));
	}

	@Test
	void createItem_WithValidData_ReturnsCreated() throws Exception {
		when(itemService.save(any(Item.class))).thenReturn(testItem);

		mockMvc.perform(post("/api/items")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(testItem)))
				.andExpect(status().isCreated());
	}

	@Test
	void getItemById_WhenExists_ReturnsItem() throws Exception {
		when(itemService.findById(1L)).thenReturn(Optional.of(testItem));

		mockMvc.perform(get("/api/items/1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(1))
				.andExpect(jsonPath("$.name").value("Test Item"));
	}

	@Test
	void updateItem_WhenExists_ReturnsOk() throws Exception {
		when(itemService.findById(1L)).thenReturn(Optional.of(testItem));
		when(itemService.save(any(Item.class))).thenReturn(testItem);

		mockMvc.perform(put("/api/items/1")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(testItem)))
				.andExpect(status().isOk());
	}

	@Test
	void deleteItem_WhenExists_ReturnsNoContent() throws Exception {
		when(itemService.findById(1L)).thenReturn(Optional.of(testItem));
		doNothing().when(itemService).deleteById(1L);

		mockMvc.perform(delete("/api/items/1"))
				.andExpect(status().isNoContent());
	}

	// Error Cases
	@Test
	void getItemById_WhenNotExists_ReturnsNotFound() throws Exception {
		when(itemService.findById(1L)).thenReturn(Optional.empty());

		mockMvc.perform(get("/api/items/1"))
				.andExpect(status().isNotFound());
	}

	@Test
	void createItem_WithInvalidEmail_ReturnsBadRequest() throws Exception {
		testItem.setEmail("invalid-email");

		mockMvc.perform(post("/api/items")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(testItem)))
				.andExpect(status().isBadRequest());
	}
}
