UNIVERSIDAD NACIONAL EXPERIMENTAL DE GUAYANA
INGENIERIA INFORMATICA
PROYECTO: DETECTOR DE PLAGIO DE CODIGO FUENTE
AUTOR: JORG SIERRA 31.782.673


README DEL PROYECTO

1. Descarga
-----------
Clonar el repositorio:

    git clone https://github.com/usuario/plagiarism-detector.git

O descargar el ZIP desde GitHub y extraerlo en una carpeta.


2. Compilacion
--------------
Abrir una terminal en la carpeta del proyecto y ejecutar:

    javac -d bin src/main/*.java src/main/model/*.java src/main/parser/*.java src/main/isomorphism/*.java src/main/gui/*.java


3. Ejecucion
------------
    java -cp bin main.Main


4. Uso Basico
-------------
1. Ingresar el codigo del Programa 1 (escribir, cargar archivo o pegar)
2. Hacer clic en "Generar Grafo"
3. Ingresar el codigo del Programa 2
4. Hacer clic en "Generar Grafo"
5. El sistema compara automaticamente y muestra el resultado

   Resultado:
   - Verde (0-50%):  Probablemente original
   - Naranja (50-70%): Similitud moderada
   - Rojo (70-100%):  Posible plagio


5. Controles del Grafo
-----------------------
- Arrastrar con el mouse: Mover la vista
- Rueda del mouse: Zoom
- Pasar mouse sobre nodo: Resalta conexiones
- Boton "Ajustar": Ver todo el grafo
- Selector "Estilo": Cambiar diseño del grafo


6. Solucion de Problemas
-------------------------
Error: "java no se reconoce"
  Solucion: Instalar JDK desde https://www.java.com

Error: "No se encuentra la clase Main"
  Solucion: Volver a compilar con el comando javac

Error: El programa se ve pequeno
  Solucion: java -Dsun.java2d.uiScale=1.5 -cp bin main.Main


7. Contacto
-----------
Autor: Jorg Sierra
Repositorio: https://github.com/AntoniXL/Detector-de-Plagio-de-C-digo-Fuente..git

FIN DEL README
