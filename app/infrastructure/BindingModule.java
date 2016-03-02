package infrastructure;

import com.google.inject.AbstractModule;

public class BindingModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(Startup.class).asEagerSingleton();
    }
}