package com.example.app_movil_gastronomia.ui.pedido;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static org.junit.Assert.assertEquals;
import android.os.Bundle;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Lifecycle;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.app_movil_gastronomia.MainActivity;
import com.example.app_movil_gastronomia.R;
import com.example.app_movil_gastronomia.core.TokenManager;
import com.example.app_movil_gastronomia.core.UiState;
import com.example.app_movil_gastronomia.data.dto.pedido.CrearPedidoRequest;
import com.example.app_movil_gastronomia.data.dto.pedido.EstadoPedidoEnum;
import com.example.app_movil_gastronomia.data.dto.pedido.PedidoDetalleDto;
import com.example.app_movil_gastronomia.data.dto.pedido.PedidoResumenDto;
import com.example.app_movil_gastronomia.data.dto.producto.ProductoDto;
import com.example.app_movil_gastronomia.data.repository.contract.PedidoRepository;
import com.example.app_movil_gastronomia.data.repository.contract.ProductoRepository;
import com.example.app_movil_gastronomia.nav.FakeTokenManager;

import org.junit.Before;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;

@HiltAndroidTest
@RunWith(AndroidJUnit4.class)
public class CajaRestrictionUiBehaviorTest {

    @Rule
    public HiltAndroidRule hiltRule = new HiltAndroidRule(this);
    public FakePedidoRepository pedidoRepository = new FakePedidoRepository();
    public ProductoRepository productoRepository = createProductoRepository();
    @Inject
    public TokenManager tokenManager;

    @Before
    public void setUp() {
        CajaRestrictionTestRepositoryModule.setRepositories(pedidoRepository, productoRepository);
        hiltRule.inject();
    }

    @After
    public void tearDown() {
        CajaRestrictionTestRepositoryModule.setRepositories(null, null);
    }

    @Test
    public void noOpenRegisterShortcut_navigatesToCajaWhenConfirmed() {
        ActivityScenario<MainActivity> scenario = launchAt(R.id.nav_crear_pedido);
        focusActivity(scenario);
        scenario.onActivity(activity -> assertEquals(
                R.id.nav_crear_pedido, currentNavController(activity).getCurrentDestination().getId()));

        scenario.onActivity(activity -> pedidoRepository.emitCreateError(
                "NO_OPEN_REGISTER", "Abrí una caja antes de crear un pedido"));
        onView(withText(R.string.open_caja_button)).perform(click());
        scenario.onActivity(activity -> assertEquals(
                R.id.nav_caja, currentNavController(activity).getCurrentDestination().getId()));
        scenario.close();
    }

    @Test
    public void cancelOrderConfirmation_changesStateAndHidesCancelAction() {
        ActivityScenario<MainActivity> scenario = launchAtDetail(73);
        scenario.onActivity(activity -> {
            android.view.View cancel = activity.findViewById(R.id.button_cancelar_pedido);
            assertEquals(android.view.View.VISIBLE, cancel.getVisibility());
            invokeCancelConfirmation(activity);
        });
        onView(withText(R.string.action_confirm)).inRoot(isDialog()).perform(click());

        assertEquals(73, pedidoRepository.lastChangedId);
        assertEquals(EstadoPedidoEnum.CANCELADO, pedidoRepository.lastChangedState);
        scenario.onActivity(activity -> assertEquals(
                android.view.View.GONE,
                activity.findViewById(R.id.button_cancelar_pedido).getVisibility()));
        scenario.close();
    }

    @Test
    public void activeOrderList_rendersOnlyOrdersReturnedForCurrentRegister() {
        ActivityScenario<MainActivity> scenario = launchAt(R.id.nav_pedido_list);
        focusActivity(scenario);
        scenario.onActivity(activity -> assertEquals(
                R.id.nav_pedido_list, currentNavController(activity).getCurrentDestination().getId()));
        PedidoResumenDto current = summary(21, "Pedido de caja actual");
        scenario.onActivity(activity ->
                pedidoRepository.emitActiveOrders(Collections.singletonList(current)));
        onView(withText("Pedido de caja actual")).check(matches(isDisplayed()));
        onView(withText("Pedido histórico")).check(doesNotExist());
        scenario.close();
    }

    private ActivityScenario<MainActivity> launchAt(int destination) {
        ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class);
        scenario.moveToState(Lifecycle.State.RESUMED);
        scenario.onActivity(activity -> {
            ((FakeTokenManager) tokenManager).setRole("cajero");
            NavController controller = currentNavController(activity);
            controller.navigate(destination);
            activity.getSupportFragmentManager().executePendingTransactions();
        });
        return scenario;
    }

    private static void focusActivity(ActivityScenario<MainActivity> scenario) {
        scenario.onActivity(activity -> {
            android.view.View decor = activity.getWindow().getDecorView();
            decor.setFocusableInTouchMode(true);
            decor.requestFocus();
        });
    }

    private ActivityScenario<MainActivity> launchAtDetail(int pedidoId) {
        ActivityScenario<MainActivity> scenario = launchAt(R.id.nav_pedido_list);
        scenario.onActivity(activity -> {
            Bundle args = new Bundle();
            args.putInt("pedidoId", pedidoId);
            currentNavController(activity).navigate(
                    R.id.action_nav_pedido_list_to_nav_pedido_detail, args);
            activity.getSupportFragmentManager().executePendingTransactions();
        });
        return scenario;
    }

    private static NavController currentNavController(MainActivity activity) {
        NavHostFragment navHost = (NavHostFragment) activity.getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment_content_main);
        if (navHost == null) {
            throw new AssertionError("NavHostFragment should be present");
        }
        return navHost.getNavController();
    }

    private static void invokeCancelConfirmation(MainActivity activity) {
        try {
            NavHostFragment navHost = (NavHostFragment) activity.getSupportFragmentManager()
                    .findFragmentById(R.id.nav_host_fragment_content_main);
            PedidoDetailFragment fragment = (PedidoDetailFragment) navHost
                    .getChildFragmentManager().getFragments().get(0);
            Method method = PedidoDetailFragment.class.getDeclaredMethod("confirmCancelOrder");
            method.setAccessible(true);
            method.invoke(fragment);
        } catch (ReflectiveOperationException | ClassCastException exception) {
            throw new AssertionError("Could not open the cancel confirmation dialog", exception);
        }
    }

    private static PedidoResumenDto summary(int id, String customer) {
        PedidoResumenDto summary = new PedidoResumenDto();
        summary.setId(id);
        summary.setEstado("Pendiente");
        summary.setClienteNombre(customer);
        summary.setMetodoVenta("Retiro en local");
        summary.setTotalEstimado(100);
        return summary;
    }

    private static PedidoDetalleDto detail(int id, String estado) {
        PedidoDetalleDto detail = new PedidoDetalleDto();
        detail.setId(id);
        detail.setEstado(estado);
        detail.setClienteNombre("Cliente de prueba");
        detail.setMetodoVenta("Retiro en local");
        detail.setTotalEstimado(100);
        detail.setDetallePedidos(Collections.emptyList());
        return detail;
    }

    public static final class FakePedidoRepository implements PedidoRepository {
        private final MutableLiveData<UiState<List<PedidoResumenDto>>> pedidosState =
                new MutableLiveData<>();
        private final MutableLiveData<UiState<PedidoDetalleDto>> pedidoState =
                new MutableLiveData<>();
        private final MutableLiveData<UiState<List<PedidoResumenDto>>> byEstadoState =
                new MutableLiveData<>();
        private final MutableLiveData<UiState<PedidoDetalleDto>> crearState =
                new MutableLiveData<>();
        private final MutableLiveData<UiState<PedidoDetalleDto>> cambiarEstadoState =
                new MutableLiveData<>();
        private final MutableLiveData<UiState<PedidoDetalleDto>> asignarState =
                new MutableLiveData<>();

        private PedidoDetalleDto detail = detail(73, "Pendiente");
        private int lastChangedId;
        private EstadoPedidoEnum lastChangedState;
        void emitCreateError(String code, String message) {
            crearState.setValue(UiState.error(message, code));
        }

        void emitActiveOrders(List<PedidoResumenDto> orders) {
            pedidosState.setValue(UiState.success(orders));
        }

        @Override
        public LiveData<UiState<List<PedidoResumenDto>>> getPedidos() {
            pedidosState.setValue(UiState.success(
                    Collections.singletonList(summary(1, "Pedido histórico"))));
            return pedidosState;
        }

        @Override
        public LiveData<UiState<List<PedidoResumenDto>>> getPedidosState() {
            return pedidosState;
        }

        @Override
        public LiveData<UiState<PedidoDetalleDto>> getPedido(int id) {
            pedidoState.setValue(UiState.success(detail));
            return pedidoState;
        }

        @Override
        public LiveData<UiState<PedidoDetalleDto>> getPedidoState() {
            return pedidoState;
        }

        @Override
        public LiveData<UiState<List<PedidoResumenDto>>> getByEstado(EstadoPedidoEnum estado) {
            byEstadoState.setValue(UiState.success(Collections.singletonList(summary(1, "Pedido activo"))));
            return byEstadoState;
        }

        @Override
        public LiveData<UiState<List<PedidoResumenDto>>> getByEstadoState() {
            return byEstadoState;
        }

        @Override
        public LiveData<UiState<PedidoDetalleDto>> crearPedido(CrearPedidoRequest request) {
            crearState.setValue(UiState.loading());
            return crearState;
        }

        @Override
        public LiveData<UiState<PedidoDetalleDto>> getCrearState() {
            return crearState;
        }

        @Override
        public void resetCrearState() {
            crearState.setValue(null);
        }

        @Override
        public LiveData<UiState<PedidoDetalleDto>> cambiarEstado(int id, EstadoPedidoEnum estado) {
            lastChangedId = id;
            lastChangedState = estado;
            detail.setEstado(estado == EstadoPedidoEnum.CANCELADO ? "Cancelado" : estado.getApiValue());
            cambiarEstadoState.setValue(UiState.success(detail));
            return cambiarEstadoState;
        }

        @Override
        public LiveData<UiState<PedidoDetalleDto>> getCambiarEstadoState() {
            return cambiarEstadoState;
        }

        @Override
        public LiveData<UiState<PedidoDetalleDto>> asignarRepartidor(int id, int repartidorId) {
            asignarState.setValue(UiState.success(detail));
            return asignarState;
        }

        @Override
        public LiveData<UiState<PedidoDetalleDto>> getAsignarRepartidorState() {
            return asignarState;
        }
    }

    private static ProductoRepository createProductoRepository() {
        MutableLiveData<UiState<List<ProductoDto>>> products = new MutableLiveData<>();
        MutableLiveData<UiState<ProductoDto>> product = new MutableLiveData<>();
        MutableLiveData<UiState<Void>> deleted = new MutableLiveData<>();
        return (ProductoRepository) Proxy.newProxyInstance(
                ProductoRepository.class.getClassLoader(),
                new Class<?>[]{ProductoRepository.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getProductos":
                            products.setValue(UiState.success(Collections.emptyList()));
                            return products;
                        case "getProductListState":
                            return products;
                        case "getProducto":
                        case "getProductoState":
                            return product;
                        case "getEliminarState":
                        case "eliminarProducto":
                            return deleted;
                        default:
                            return new MutableLiveData<>();
                    }
                });
    }
}
