package com.dexburger.order.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.dexburger.burgers.dto.BurgerDTO;
import com.dexburger.burgers.factory.BurgerFactory;
import com.dexburger.burgers.model.Burger;
import com.dexburger.exceptions.BurgerNotFoundException;
import com.dexburger.exceptions.IngredientNotFoundException;
import com.dexburger.exceptions.OrderNotFoundException;
import com.dexburger.ingredients.dto.IngredientDTO;
import com.dexburger.ingredients.factory.IngredientFactory;
import com.dexburger.ingredients.model.Ingredient;
import com.dexburger.menu.repositories.IngredientRepository;
import com.dexburger.order.dto.OrderDTO;
import com.dexburger.order.model.Order;
import com.dexburger.order.repository.OrderRepository;
import com.dexburger.prices.service.PriceService;

@Service
public class OrderServiceImpl implements OrderService {

	private BurgerFactory burgerFactory;
	private IngredientFactory ingredientFactory;
	private OrderRepository orderRepository;
	private PriceService priceService;

	@Autowired
	public OrderServiceImpl(BurgerFactory burgerFactory, IngredientFactory ingredientFactory,
			OrderRepository orderRepository, PriceService priceService) {
		this.burgerFactory = burgerFactory;
		this.ingredientFactory = ingredientFactory;
		this.orderRepository = orderRepository;
		this.priceService = priceService;
	}

	/**
	 * burgersDTOtoBurgers
	 * 
	 * @param burgersDTO List<BurgerDTO>
	 * @return List<Burger>
	 */
	private List<Burger> burgersDTOtoBurgers(final List<BurgerDTO> burgersDTO) {
		List<Burger> burgers = new ArrayList<Burger>();

		for (BurgerDTO burgerDTO : burgersDTO) {
			burgers.add(burgerDTOtoBurger(burgerDTO));
		}

		return burgers;
	}

	/**
	 * burgerDTOtoBurger
	 * 
	 * @param burgerDTO BurgerDTO
	 * @return Burger
	 */
	private Burger burgerDTOtoBurger(final BurgerDTO burgerDTO) {
		Burger burger = burgerFactory.create(burgerDTO.get_id());

		if (!CollectionUtils.isEmpty(burgerDTO.getExtras())) {
			List<Ingredient> extras = ingredientsDTOtoIngredients(burgerDTO.getExtras());
			for (Ingredient extra : extras)
				burger.addIngredient(extra);
		}

		calculateFinalPrice(burger);

		return burger;
	}

	/**
	 * applyDiscount
	 * 
	 * @param burger Burger
	 */
	private void calculateFinalPrice(Burger burger) {
		priceService.calculatePrice(burger);
		priceService.applyDiscounts(burger);
	}

	/**
	 * ingredientsDTOtoIngredients
	 * 
	 * @param ingredientsDTO List<IngredientDTO>
	 * @return List<Ingredient>
	 */
	private List<Ingredient> ingredientsDTOtoIngredients(final List<IngredientDTO> ingredientsDTO) {
		List<Ingredient> ingredients = new ArrayList<>();

		for (IngredientDTO ingredientDTO : ingredientsDTO) {
			Ingredient ingredient = IngredientRepository.getInstance(ingredientFactory).findById(ingredientDTO.get_id())
					.orElseThrow(() -> new IngredientNotFoundException(ingredientDTO.get_id()));
			ingredients.add(ingredient);
		}

		return ingredients;
	}

	@Override
	public Order addOrder(final OrderDTO orderDTO) {
		List<BurgerDTO> burgersDTO = orderDTO.getBurgers();

		if (CollectionUtils.isEmpty(burgersDTO))
			throw new BurgerNotFoundException();

		Order order = new Order();
		order.setBurgers(burgersDTOtoBurgers(burgersDTO));
		priceService.calculatePrice(order);
		orderRepository.add(order);
		return order;
	}

	@Override
	public Order getOrder(final Long orderId) {
		return orderRepository.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
	}

	@Override
	public Order updateOrder(final Long orderId, final OrderDTO orderDTO) {
		List<BurgerDTO> burgersDTO = orderDTO.getBurgers();

		if (CollectionUtils.isEmpty(burgersDTO))
			throw new BurgerNotFoundException();

		Order order = getOrder(orderId);
		order.setBurgers(burgersDTOtoBurgers(burgersDTO));
		priceService.calculatePrice(order);
		orderRepository.update(order);
		return order;
	}

	@Override
	public void deleteOrder(final Long orderId) {
		orderRepository.remove(getOrder(orderId));
	}

	@Override
	public Collection<Order> findAll() {
		final List<Order> orders = orderRepository.findAll();

		if (CollectionUtils.isEmpty(orders))
			throw new OrderNotFoundException();

		return orders;
	}

	@Override
	public Collection<Burger> getBurgersByOrder(final Long orderId) {
		Order order = getOrder(orderId);

		List<Burger> burgers = order.getBurgers();

		if (CollectionUtils.isEmpty(burgers))
			throw new BurgerNotFoundException();

		return burgers;
	}

}
