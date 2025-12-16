package it.pagopa.pn.deliverypush;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;
import org.springframework.util.ClassUtils;

import java.io.IOException;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class TimelineDetailsServerCounter {
    private static final String BASE_PACKAGE = "it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto";
    private static final String INTERFACE_NAME = "it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElementDetailsV27";

    public static int countSubClasses() throws IOException, ClassNotFoundException {
        // 1. Definisci il pattern di scansione
        String packageSearchPath = "classpath*:" + BASE_PACKAGE.replace('.', '/') + "/**/*.class";

        // 2. Resolver per trovare le risorse (.class files)
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources(packageSearchPath);

        // 3. Factory per leggere i metadati (senza caricare la classe)
        SimpleMetadataReaderFactory metadataReaderFactory = new SimpleMetadataReaderFactory();
        Set<Class<?>> implementingClasses = new HashSet<>();

        // 4. Itera e filtra
        for (Resource resource : resources) {
            if (resource.isReadable()) {
                MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(resource);

                // Salta le interfacce e le classi astratte (se l'interfaccia base è astratta)
                if (metadataReader.getClassMetadata().isConcrete()) {

                    String[] interfaceNames = metadataReader.getClassMetadata().getInterfaceNames();

                    // Verifica se implementa l'interfaccia target
                    for (String iface : interfaceNames) {
                        if (iface.equals(INTERFACE_NAME)) {
                            // Carica la classe e aggiungila al set (opzionale, ma utile per ispezioni)
                            String className = metadataReader.getClassMetadata().getClassName();
                            implementingClasses.add(Objects.requireNonNull(ClassUtils.getDefaultClassLoader()).loadClass(className));
                            break;
                        }
                    }
                }
            }
        }

        return implementingClasses.size();
    }

}
