package com.example.demo.view;

import com.example.demo.entity.Guestbook;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.shared.Registration;

public class GuestbookForm extends VerticalLayout {

    private final TextField name = new TextField("Name");
    private final EmailField email = new EmailField("Email");
    private final TextArea comment = new TextArea("Comment");
    
    private final Button save = new Button("Save");
    private final Button delete = new Button("Delete");
    private final Button close = new Button("Cancel");
    
    private final Binder<Guestbook> binder = new BeanValidationBinder<>(Guestbook.class);
    private Guestbook entry;

    public GuestbookForm() {
        addClassName("guestbook-form");
        
        H3 title = new H3("Entry Details");
        
        // Configure fields
        name.setRequired(true);
        name.setMaxLength(100);
        name.setHelperText("Maximum 100 characters");
        
        email.setRequired(true);
        email.setMaxLength(150);
        email.setHelperText("Valid email address required");
        
        comment.setRequired(true);
        comment.setMaxLength(1000);
        comment.setHelperText("Maximum 1000 characters");
        comment.setHeight("100px");
        
        // Bind fields to entity
        binder.bindInstanceFields(this);
        
        // Configure buttons
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        save.setIcon(VaadinIcon.CHECK.create());
        save.addClickShortcut(Key.ENTER);
        save.addClickListener(event -> validateAndSave());
        
        delete.addThemeVariants(ButtonVariant.LUMO_ERROR);
        delete.setIcon(VaadinIcon.TRASH.create());
        delete.addClickListener(event -> fireEvent(new DeleteEvent(this, entry)));
        
        close.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        close.setIcon(VaadinIcon.CLOSE.create());
        close.addClickShortcut(Key.ESCAPE);
        close.addClickListener(event -> fireEvent(new CloseEvent(this)));
        
        HorizontalLayout buttonLayout = new HorizontalLayout(save, delete, close);
        buttonLayout.addClassName("button-layout");
        
        FormLayout formLayout = new FormLayout();
        formLayout.add(name, email, comment);
        formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        
        add(title, formLayout, buttonLayout);
        
        setWidth("25em");
    }

    private void validateAndSave() {
        try {
            binder.writeBean(entry);
            fireEvent(new SaveEvent(this, entry));
        } catch (ValidationException e) {
            // Validation errors are shown automatically in the form
        }
    }

    public void editEntry(Guestbook entry) {
        if (entry == null) {
            setVisible(false);
        } else {
            setVisible(true);
            this.entry = entry;
            binder.readBean(entry);
            
            // Show delete button only for existing entries
            delete.setVisible(entry.getId() != null);
        }
    }

    // Events
    public static abstract class GuestbookFormEvent extends ComponentEvent<GuestbookForm> {
        private Guestbook entry;

        protected GuestbookFormEvent(GuestbookForm source, Guestbook entry) {
            super(source, false);
            this.entry = entry;
        }

        public Guestbook getEntry() {
            return entry;
        }
    }

    public static class SaveEvent extends GuestbookFormEvent {
        SaveEvent(GuestbookForm source, Guestbook entry) {
            super(source, entry);
        }
    }

    public static class DeleteEvent extends GuestbookFormEvent {
        DeleteEvent(GuestbookForm source, Guestbook entry) {
            super(source, entry);
        }
    }

    public static class CloseEvent extends GuestbookFormEvent {
        CloseEvent(GuestbookForm source) {
            super(source, null);
        }
    }

    public Registration addDeleteListener(ComponentEventListener<DeleteEvent> listener) {
        return addListener(DeleteEvent.class, listener);
    }

    public Registration addSaveListener(ComponentEventListener<SaveEvent> listener) {
        return addListener(SaveEvent.class, listener);
    }

    public Registration addCloseListener(ComponentEventListener<CloseEvent> listener) {
        return addListener(CloseEvent.class, listener);
    }
}