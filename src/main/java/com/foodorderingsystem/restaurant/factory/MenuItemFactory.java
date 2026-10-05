package com.foodorderingsystem.restaurant.factory;

import com.foodorderingsystem.restaurant.entity.MenuItem;
import org.springframework.stereotype.Component;

// Factory pattern: the admin "Add Menu Item" form can quick-start from a category template
// instead of a blank form. The caller only says WHICH category it wants ("Rice",
// "Short Eats", ...) - this factory decides what a sensible default name/price/description/
// image looks like for that category, so RestaurantController never has to know those details.
@Component
public class MenuItemFactory {

    public MenuItem createDefaultItem(String category) {
        if (category == null) {
            return blankItem();
        }
        switch (category) {
            case "Rice":
                return build("Rice", "Fried Rice", "Stir-fried rice with vegetables and your choice of protein.",
                        750.0, "https://images.unsplash.com/photo-1603133872878-684f208fb84b");
            case "Short Eats":
                return build("Short Eats", "Chicken Roll", "Crispy pastry roll filled with spiced chicken.",
                        150.0, "https://images.unsplash.com/photo-1601050690597-df0568f70950");
            case "Drinks":
                return build("Drinks", "Fresh Fruit Juice", "Chilled, freshly squeezed juice.",
                        300.0, "https://images.unsplash.com/photo-1600271886742-f049cd451bba");
            case "Desserts":
                return build("Desserts", "Watalappan", "Traditional Sri Lankan coconut custard pudding.",
                        250.0, "https://images.unsplash.com/photo-1551024506-0bccd828d307");
            default:
                return blankItem();
        }
    }

    private MenuItem build(String category, String name, String description, double price, String imageUrl) {
        MenuItem item = new MenuItem();
        item.setCategory(category);
        item.setName(name);
        item.setDescription(description);
        item.setPrice(price);
        item.setImageUrl(imageUrl);
        item.setAvailable(true);
        return item;
    }

    private MenuItem blankItem() {
        return new MenuItem();
    }
}
