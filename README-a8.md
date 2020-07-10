# Publish Steps

## Scala JVM
- `sbt ";+ clean;+ publish"`

## Scala JS 0.6
- Uncomment the `jsDependencyManifest` line in `build.sbt`
- `sbt -DscalaJSVersion=0.6.33 ";+ clean;project core;+ publish"`

## Scala JS 1
- `sbt -DscalaJSVersion=1.1.1 ";+ clean;+ publish"`
