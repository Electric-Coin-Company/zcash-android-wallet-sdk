package co.electriccoin.lightwallet.client.model

sealed class Response<T> {
    data class Success<T>(val result: T) : Response<T>()

    sealed class Failure<T> : Response<T>() {

        /**
         * The client was not able to communicate with the server.
         */
        class Connection<T> : Failure<T>()

        /**
         * The server did respond and returned an error.
         */
        class Server<T> : Failure<T>()

        /**
         * A failure occurred on the client, such as parsing a response failed.
         */
        class Client<T> : Failure<T>()
    }
}
