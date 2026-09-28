package restaurante;

import java.nio.file.Path;
import restaurante.config.Configuracion;
import restaurante.coordinacion.Simulacion;

public final class Main {
    private Main() { }
    public static void main(String[] args) throws InterruptedException {
        if (args.length > 1) throw new IllegalArgumentException("Uso: restaurante.Main [archivo-log]");
        Path log = Path.of(args.length == 1 ? args[0] : "output/simulacion.log");
        new Simulacion(Configuracion.predeterminados(), log).ejecutar();
    }
}
