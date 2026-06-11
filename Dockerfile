# Stage 1: Build
FROM mcr.microsoft.com/dotnet/sdk:10.0 AS build
WORKDIR /src

# Restore de paquetes (aprovechando cache de capa)
COPY ApiGastronomia.csproj .
RUN dotnet restore ApiGastronomia.csproj

# Copia el resto y publica
COPY . .
RUN dotnet publish ApiGastronomia.csproj -c Release -o /app/publish --no-restore

# Stage 2: Runtime
FROM mcr.microsoft.com/dotnet/aspnet:10.0 AS runtime
WORKDIR /app

# Copia los binarios publicados
COPY --from=build /app/publish .

# Expone el puerto que usa la app (Program.cs usa http://+:80)
EXPOSE 80

ENTRYPOINT ["dotnet", "ApiGastronomia.dll"]
