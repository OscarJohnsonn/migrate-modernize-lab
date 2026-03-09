using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Design;
using Microsoft.Extensions.Configuration;

namespace ContosoUniversity.Data
{
    /// <summary>
    /// Design-time factory for EF Core migrations
    /// </summary>
    public class SchoolContextFactory : IDesignTimeDbContextFactory<SchoolContext>
    {
        public SchoolContext CreateDbContext(string[] args)
        {
            var optionsBuilder = new DbContextOptionsBuilder<SchoolContext>();
            
            // Build configuration to read connection string
            IConfigurationRoot configuration = new ConfigurationBuilder()
                .SetBasePath(Directory.GetCurrentDirectory())
                .AddJsonFile("appsettings.json")
                .AddJsonFile("appsettings.Development.json", optional: true)
                .AddEnvironmentVariables()
                .Build();
            
            var connectionString = configuration.GetConnectionString("DefaultConnection");
            
            // Use SQL Server for Azure deployment
            // At runtime, DI will provide the actual connection string from appsettings.json
            optionsBuilder.UseSqlServer(connectionString ?? "Server=tcp:localhost;Database=ContosoUniversity;Trusted_Connection=True;TrustServerCertificate=True");
            
            return new SchoolContext(optionsBuilder.Options);
        }
    }
}
