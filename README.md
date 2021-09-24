## Three.js Facades

### Current `@types/three` Version: 131.0

This package provides a set of [facade types](https://www.scala-js.org/doc/interoperability/facade-types.html) for ScalaJS users to be able to use [THREE.js](https://threejs.org/) from Scala. They are generated by [this script](https://github.com/dcascaval/parse-types), which uses the `@types/three` NPM package and programmatically converts the Typescript definitions into ScalaJS ones. The generated definitions target Scala 3, and ScalaJS 1.7.0+ compiled with JDK 11.

Note that this includes the entirety of the THREE.js core, but only `OrbitControls` from their included examples. The examples include a lot of useful code, and more should be ported over -- however, the type definitions are much less robust than those from the core, and this represents a larger lift.

Absolutely no guarantees are made about reliability here; steal 'em if they work for you.

### Usage

Full example of project setup: https://github.com/dcascaval/scala-threejs-facades-example

- Scala:
  - Add `addSbtPlugin("com.codecommit" % "sbt-github-packages" % "0.5.3")` to `project/plugins.sbt`
  - Add `libraryDependencies += "org.cascaval" %%% "three-typings" % "0.131.0-SNAPSHOT"` to `build.sbt`.
  - export a `GITHUB_TOKEN` environment variable that can read from the Github Package Registry (create one at `Settings > Developer Settings > Personal Access Tokens` with the `read:packages` permission). For documentation on other ways to provide authentication, see the [sbt-github-packages documentation](https://github.com/djspiewak/sbt-github-packages).
 
- JS:
  - Use webpack to package `three` into a bundle along with your other dependencies, or simply include `three.min.js` (along with any of the examples files you want to use, e.g. `OrbitControls`) in your html.
  - `three.min.js` can be obtained from `node_modules` after running `npm install` or `yarn add`, or directly [from the source](https://github.com/mrdoob/three.js/blob/dev/build/three.min.js)
  - Sometimes including the following snippet is needed to make everything play nicely.
    ```html
    <script>
      window.global = window;
    </script>
    ```

### Goals

Other tools, such as [ScalablyTyped](https://scalablytyped.org/docs/readme.html) exist and can usually handle this type of Typescript -> ScalaJS Facade conversion. In this case, however, the purely automatic conversion runs into trouble (several different conflicting versions of many base classes are generated, rendering it difficult to call objects in one module of THREE with objects from another). The goals of this package are to:

- Generate facade types that are specifically designed to work with the way that `THREE.js` is laid out
- Programatically apply special cases in such a way that keeping up-to-date involves minimal work, but that the ergonomics are close to that of a hand-written facade.
- Target Scala 3 specifically, and ultimately provide helpful extension methods for working with THREE objects in a fluent style.

### Drawbacks

This project is functional (compiles and runs!), and replicates several of the basic THREE.js examples with minimal casting and no namespacing issues. That said, this project does not resemble anything production-ready. Primarily this is because:

- There are no tests, let alone automated tests, to verify that the API surface is covered correctly.
- As described above these bindings assume _all_ of THREE.js is in the global scope, as opposed to using modules and allowing tree-shaking to do its thing. Minified, the core `three.min.js` is 602kb at the time of writing. Moreover you will need to include any code from [the examples](https://github.com/mrdoob/three.js/tree/dev/examples/js) manually. This includes a number of features you might want, like `OrbitControls`.
- There is no guarantee that any of the above will ever be supported
- There are some cases where mutating super types can break type safety (see below.)

### Special Cases

Interfaces to objects that serve as JS parameters are represented in TypeScript as buckets of potentially undefined fields.

```typescript
export interface PointsMaterialParameters extends MaterialParameters {
  color?: ColorRepresentation | undefined;
  map?: Texture | null | undefined;
  alphaMap?: Texture | null | undefined;
  size?: number | undefined;
  sizeAttenuation?: boolean | undefined;
}

export class PointsMaterial extends Material {
  constructor(parameters?: PointsMaterialParameters);
  // ...
}
```

When instantiating these in scala, it's nice to be able to allow the following call site syntax, not mentioning the name of the interface or the other parameters.

```scala
new PointsMaterial(new { size = 0.1; color = "#FFF" })
```

Here Scala infers the type and instantiates an anonymous trait instance with the properties we want. However, naively translating the definition straight from TypeScript will allow the `parameters` argument to be optional, and type inference fails because `PointsMaterial` expects `js.UndefOr[PointsMaterialParameters]` instead of `PointsMaterialParameters`. In these bindings we special-case these points.

### Subtyping Safety

A number of the THREE typings contain subtyping of the following form:

```javascript
class Light {
  camera: Camera;
}

class SubCamera extends Camera {}

class SubLight extends Light {
  camera: SubCamera;
}
```

Naively this translates to:

```scala
class Light {
  var camera: Camera
}

class SubCamera extends Camera {}

class SubLight extends Light {
  override var camera: SubCamera
}
```

but Scala does not allow us to override `var`s with subtypes. To some degree this is fundamental: if it did, we could set the `camera` field to a different subtype of Camera than `SubLight` expects, breaking type safety. Instead the approach we currently take is to not override the method, and if you need to access it _as_ a subtype, a hard cast must be performed. As follows:

```scala
class Light {
  var camera: Camera
}

class SubCamera extends Camera {}
class SubLight extends Light {}

// Usage
val l = SubLight()
val c : SubCamera = l.camera.asInstanceOf[SubCamera]
```

This isn't particularly typesafe either (we haven't changed the underlying JS API at all) but:

- As opposed to the naive translation, it compiles and runs
- It makes assumptions about types explicit -- and you can always return an `Option` or union type via an extension method instead of casting directly:

  ```scala
  extension (l: SubLight)
    def subCamera: Option[SubCamera] =
      if l.camera.isInstanceOf[SubCamera] then
        Some(l.camera.asInstanceOf[SubCamera])
      else None

  // Usage
  val l = SubLight()
  val c: Option[SubCamera] = l.subCamera
  ```

  I think it's reasonable for this library to generate code like the above in the future.

- In practice, needing to access the subtype-specific attributes of this type of field seems rare. (Again, this library is not production material.)
