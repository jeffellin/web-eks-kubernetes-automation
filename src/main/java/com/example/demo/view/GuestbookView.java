package com.example.demo.view;

import com.example.demo.config.GuestbookProperties;
import com.example.demo.entity.Guestbook;
import com.example.demo.service.GuestbookService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import com.vaadin.flow.theme.lumo.Lumo;
import com.vaadin.flow.component.AttachEvent;
import org.springframework.beans.factory.annotation.Autowired;

@SpringComponent
@UIScope
@Route("")
public class GuestbookView extends VerticalLayout {

    private final GuestbookService guestbookService;
    private final GuestbookProperties guestbookProperties;
    private final Grid<Guestbook> grid;
    private final GuestbookForm form;
    private final TextField filterText;

    @Autowired
    public GuestbookView(GuestbookService guestbookService, GuestbookProperties guestbookProperties) {
        this.guestbookService = guestbookService;
        this.guestbookProperties = guestbookProperties;
        
        
        addClassName("guestbook-view");
        setSizeFull();
        
        // Create components
        H1 title = new H1("Guestbook@!!!");
        title.addClassName("guestbook-title");
        
        filterText = new TextField();
        filterText.setPlaceholder("Filter by name...");
        filterText.setClearButtonVisible(true);
        filterText.setValueChangeMode(ValueChangeMode.LAZY);
        filterText.addValueChangeListener(e -> updateList());
        
        grid = new Grid<>(Guestbook.class, false);
        configureGrid();
        
        form = new GuestbookForm();
        form.setVisible(false);
        form.addSaveListener(this::saveEntry);
        form.addDeleteListener(this::deleteEntry);
        form.addCloseListener(e -> closeEditor());
        
        Button addEntryButton = new Button("Add Entry");
        addEntryButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addEntryButton.setIcon(VaadinIcon.PLUS.create());
        addEntryButton.addClickListener(e -> {
            grid.asSingleSelect().clear();
            form.editEntry(new Guestbook());
        });
        
        Button themeToggleButton = new Button();
        updateThemeToggleButton(themeToggleButton);
        themeToggleButton.addClickListener(e -> toggleTheme(themeToggleButton));
        
        HorizontalLayout toolbar = new HorizontalLayout(filterText, addEntryButton, themeToggleButton);
        toolbar.addClassName("toolbar");
        
        HorizontalLayout content = new HorizontalLayout(grid, form);
        content.setFlexGrow(2, grid);
        content.setFlexGrow(1, form);
        content.addClassNames("content");
        content.setSizeFull();
        
        add(title, toolbar, content);
        updateList();
        closeEditor();
    }
    
    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        
        // Apply theme based on configuration
        if (guestbookProperties.isDarkTheme()) {
            getUI().ifPresent(ui -> ui.getElement().setAttribute("theme", Lumo.DARK));
        } else {
            getUI().ifPresent(ui -> ui.getElement().setAttribute("theme", Lumo.LIGHT));
        }
    }
    
    private void configureGrid() {
        grid.addClassName("guestbook-grid");
        grid.setSizeFull();
        
        grid.addColumn(Guestbook::getName).setHeader("Name").setSortable(true);
        grid.addColumn(Guestbook::getEmail).setHeader("Email").setSortable(true);
        grid.addColumn(Guestbook::getComment).setHeader("Comment").setFlexGrow(2);
        grid.addColumn(entry -> entry.getCreatedAt().toLocalDate().toString())
            .setHeader("Date").setSortable(true);
            
        // Add delete button column
        grid.addComponentColumn(entry -> {
            Button deleteButton = new Button(VaadinIcon.TRASH.create());
            deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);
            deleteButton.getElement().setAttribute("aria-label", "Delete entry");
            deleteButton.addClickListener(e -> confirmDelete(entry));
            return deleteButton;
        }).setHeader("Actions").setFlexGrow(0).setWidth("120px");
        
        grid.getColumns().forEach(col -> col.setAutoWidth(true));
        
        grid.asSingleSelect().addValueChangeListener(event -> editEntry(event.getValue()));
    }
    
    private void saveEntry(GuestbookForm.SaveEvent event) {
        try {
            guestbookService.save(event.getEntry());
            updateList();
            closeEditor();
            Notification notification = Notification.show("Entry saved successfully!");
            notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } catch (Exception e) {
            Notification notification = Notification.show("Error saving entry: " + e.getMessage());
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
    
    private void deleteEntry(GuestbookForm.DeleteEvent event) {
        confirmDelete(event.getEntry());
    }
    
    private void confirmDelete(Guestbook entry) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Delete Entry");
        dialog.setText(String.format("Are you sure you want to delete the entry by '%s'?", entry.getName()));
        dialog.setCancelable(true);
        dialog.setConfirmText("Delete");
        dialog.setConfirmButtonTheme("error primary");
        
        dialog.addConfirmListener(e -> performDelete(entry));
        dialog.open();
    }
    
    private void performDelete(Guestbook entry) {
        try {
            guestbookService.delete(entry);
            updateList();
            closeEditor();
            Notification notification = Notification.show("Entry deleted successfully!");
            notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } catch (Exception e) {
            Notification notification = Notification.show("Error deleting entry: " + e.getMessage());
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
    
    private void editEntry(Guestbook entry) {
        if (entry == null) {
            closeEditor();
        } else {
            form.editEntry(entry);
            form.setVisible(true);
            addClassName("editing");
        }
    }
    
    private void closeEditor() {
        form.editEntry(null);
        form.setVisible(false);
        removeClassName("editing");
    }
    
    private void updateList() {
        if (filterText.getValue() == null || filterText.getValue().trim().isEmpty()) {
            grid.setItems(guestbookService.findAll());
        } else {
            grid.setItems(guestbookService.findByNameContaining(filterText.getValue()));
        }
    }
    
    private void updateThemeToggleButton(Button themeToggleButton) {
        if (isDarkThemeActive()) {
            themeToggleButton.setText("Light Mode");
            themeToggleButton.setIcon(VaadinIcon.SUN_O.create());
            themeToggleButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        } else {
            themeToggleButton.setText("Dark Mode");
            themeToggleButton.setIcon(VaadinIcon.MOON_O.create());
            themeToggleButton.removeThemeVariants(ButtonVariant.LUMO_CONTRAST);
        }
    }
    
    private void toggleTheme(Button themeToggleButton) {
        getUI().ifPresent(ui -> {
            boolean currentlyDark = isDarkThemeActive();
            if (currentlyDark) {
                ui.getElement().setAttribute("theme", Lumo.LIGHT);
            } else {
                ui.getElement().setAttribute("theme", Lumo.DARK);
            }
            updateThemeToggleButton(themeToggleButton);
        });
    }
    
    private boolean isDarkThemeActive() {
        return getUI().map(ui -> 
            Lumo.DARK.equals(ui.getElement().getAttribute("theme"))
        ).orElse(guestbookProperties.isDarkTheme());
    }
}