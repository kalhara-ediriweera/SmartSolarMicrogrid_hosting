# IIS deployment checklist

1. Install the .NET 8 Hosting Bundle on Windows Server.
2. Publish:
   `dotnet publish SmartSolarMicrogrid.API/SmartSolarMicrogrid.API.csproj -c Release -o publish`
3. Create an IIS site/application pointing at the publish folder.
4. Configure the application pool as "No Managed Code".
5. Set production MongoDB connection string and JWT key using environment variables or secure configuration.
6. Ensure the server can reach MongoDB.
7. Do not expose MongoDB publicly; allow only the API server to access it.
8. Test `/api/health` and `/swagger`.
