<a class="btn btn-primary" href="https://metosin.github.io/reitit/basics">learn more about routing »</a>

#### Managing your middleware

Request middleware functions are located under the `competite.middleware` namespace.

This namespace is reserved for any custom middleware for the application. Some default middleware is
already defined here. The middleware is assembled in the `wrap-base` function.

Middleware used for development is placed in the `competite.dev-middleware` namespace found in
the `env/dev/clj/` source path.

<a class="btn btn-primary" href="http://www.luminusweb.net/docs/middleware.md">learn more about middleware »</a>
